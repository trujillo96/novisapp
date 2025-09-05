package com.example.novisapp.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.example.novisapp.entity.DocumentCategory;
import com.example.novisapp.entity.Document;
import com.example.novisapp.entity.LegalCase;
import com.example.novisapp.entity.User;
import com.example.novisapp.repository.DocumentRepository;
import com.example.novisapp.repository.LegalCaseRepository;
import com.example.novisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio completo para gestión documental con Azure Blob Storage
 * Funcionalidades avanzadas: upload, download, versionado, auditoría
 * VERSIÓN ACTUALIZADA - Con soporte para documentos sin caso asignado
 * Estructura de paquete plano
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final UserRepository userRepository;
    private final BlobServiceClient blobServiceClient;

    @Value("${novis.documents.max-file-size:52428800}") // 50MB
    private long maxFileSize;

    @Value("${novis.documents.max-files-per-case:200}")
    private int maxFilesPerCase;

    @Value("${novis.documents.allowed-types:PDF,DOCX,XLSX,TXT,JPG,JPEG,PNG,GIF}")
    private String allowedTypes;

    @Value("${azure.storage.container.documents:novis-documents}")
    private String documentsContainer;

    @Value("${azure.storage.container.images:novis-images}")
    private String imagesContainer;

    // ==========================================
    // MÉTODOS PRINCIPALES DE UPLOAD
    // ==========================================

    /**
     * Subir archivo único
     */
    public Document uploadDocument(Long caseId, Long userId, MultipartFile file,
                                   DocumentCategory category, String description,
                                   HttpServletRequest request) throws IOException {

        log.info("Iniciando upload de documento: {} para caso: {}", file.getOriginalFilename(), caseId);

        // Validaciones previas
        validateFile(file);

        // Solo validar capacidad si hay caso asignado
        if (caseId != null) {
            validateCaseCapacity(caseId);
        }

        // Obtener entidades (caso puede ser null)
        LegalCase legalCase = null;
        if (caseId != null) {
            legalCase = legalCaseRepository.findById(caseId)
                    .orElseThrow(() -> new RuntimeException("Caso legal no encontrado: " + caseId));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));

        // Verificar duplicados por hash (global o por caso)
        String fileHash = calculateFileHash(file.getBytes());
        Optional<Document> existingDoc = caseId != null ?
                documentRepository.findByCaseIdAndFileHash(caseId, fileHash) :
                documentRepository.findByFileHash(fileHash);

        if (existingDoc.isPresent()) {
            log.warn("Archivo duplicado detectado: {}", file.getOriginalFilename());
            throw new RuntimeException("El archivo ya existe" + (caseId != null ? " en este caso" : " en el sistema"));
        }

        // Generar nombres únicos
        String blobName = generateUniqueBlobName(file.getOriginalFilename());
        String containerName = getContainerForFileType(getFileExtension(file.getOriginalFilename()));

        // Subir a Azure Blob Storage
        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(containerName)
                .getBlobClient(blobName);

        // Configurar headers HTTP
        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(file.getContentType())
                .setContentDisposition("attachment; filename=\"" + file.getOriginalFilename() + "\"");

        // Upload del archivo
        blobClient.upload(new ByteArrayInputStream(file.getBytes()), file.getSize(), true);
        blobClient.setHttpHeaders(headers);

        // Crear entidad Document
        Document document = Document.builder()
                .fileName(generateSafeFileName(file.getOriginalFilename()))
                .originalFileName(file.getOriginalFilename())
                .fileType(getFileExtension(file.getOriginalFilename()).toUpperCase())
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .blobUrl(blobClient.getBlobUrl())
                .containerName(containerName)
                .blobName(blobName)
                .category(category != null ? category : DocumentCategory.inferFromFileName(file.getOriginalFilename()))
                .description(description)
                .version(1)
                .fileHash(fileHash)
                .legalCase(legalCase) // Puede ser null
                .uploadedBy(user)
                .uploadIpAddress(getClientIpAddress(request))
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .build();

        Document savedDocument = documentRepository.save(document);
        log.info("Documento subido exitosamente: ID={}, Blob={}", savedDocument.getId(), blobName);

        return savedDocument;
    }

    /**
     * Subir múltiples archivos
     */
    public List<Document> uploadMultipleDocuments(Long caseId, Long userId,
                                                  List<MultipartFile> files,
                                                  DocumentCategory defaultCategory,
                                                  HttpServletRequest request) throws IOException {

        log.info("Iniciando upload múltiple: {} archivos para caso: {}", files.size(), caseId);

        // Validar capacidad total solo si hay caso asignado
        if (caseId != null) {
            long currentCount = documentRepository.countActiveByCaseId(caseId);
            if (currentCount + files.size() > maxFilesPerCase) {
                throw new RuntimeException("Se excedería el límite de archivos por caso (" + maxFilesPerCase + ")");
            }
        }

        List<Document> uploadedDocuments = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                DocumentCategory category = defaultCategory != null ?
                        defaultCategory : DocumentCategory.inferFromFileName(file.getOriginalFilename());

                Document document = uploadDocument(caseId, userId, file, category,
                        "Subida múltiple - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        request);

                uploadedDocuments.add(document);
            } catch (Exception e) {
                log.error("Error subiendo archivo {}: {}", file.getOriginalFilename(), e.getMessage());
                errors.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            log.warn("Upload múltiple completado con errores: {}", errors);
        }

        log.info("Upload múltiple completado: {}/{} archivos exitosos", uploadedDocuments.size(), files.size());
        return uploadedDocuments;
    }

    // ==========================================
    // MÉTODOS DE DESCARGA
    // ==========================================

    /**
     * Descargar documento por ID
     */
    public byte[] downloadDocument(Long documentId, Long userId) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + documentId));

        if (!document.getIsActive()) {
            throw new RuntimeException("El documento no está disponible");
        }

        // Registrar acceso
        document.incrementAccessCount();
        documentRepository.save(document);

        // Descargar de Azure Blob
        BlobClient blobClient = blobServiceClient
                .getBlobContainerClient(document.getContainerName())
                .getBlobClient(document.getBlobName());

        log.info("Descargando documento: ID={}, Usuario={}", documentId, userId);
        return blobClient.downloadContent().toBytes();
    }

    /**
     * Obtener URL de descarga directa (temporal)
     */
    public String getDownloadUrl(Long documentId, int expirationMinutes) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + documentId));

        return document.getBlobUrl();
    }

    // ==========================================
    // MÉTODOS DE CONSULTA - ACTUALIZADOS CON FLEXIBILIDAD
    // ==========================================

    /**
     * Listar TODOS los documentos activos (sin filtro de caso)
     */
    @Transactional(readOnly = true)
    public List<Document> getAllDocuments() {
        return documentRepository.findAllActive();
    }

    /**
     * Listar TODOS los documentos con paginación
     */
    @Transactional(readOnly = true)
    public Page<Document> getAllDocuments(Pageable pageable) {
        return documentRepository.findAllActive(pageable);
    }

    /**
     * Listar documentos por caso (método original)
     */
    @Transactional(readOnly = true)
    public List<Document> getDocumentsByCase(Long caseId) {
        if (caseId == null) {
            return getAllDocuments();
        }
        return documentRepository.findActiveByCaseId(caseId);
    }

    /**
     * Método flexible para obtener documentos
     */
    @Transactional(readOnly = true)
    public List<Document> getDocumentsFlexible(Long caseId, boolean includeOrphans) {
        if (caseId == null) {
            return documentRepository.findAllActive();
        } else if (includeOrphans) {
            return documentRepository.findByCaseIdOrNull(caseId);
        } else {
            return documentRepository.findActiveByCaseId(caseId);
        }
    }

    /**
     * Listar documentos con paginación
     */
    @Transactional(readOnly = true)
    public Page<Document> getDocumentsByCase(Long caseId, Pageable pageable) {
        if (caseId == null) {
            return getAllDocuments(pageable);
        }
        return documentRepository.findActiveByCaseId(caseId, pageable);
    }

    /**
     * Buscar documentos por categoría (flexible)
     */
    @Transactional(readOnly = true)
    public List<Document> getDocumentsByCategory(Long caseId, DocumentCategory category) {
        if (caseId == null) {
            return documentRepository.findByCategory(category);
        }
        return documentRepository.findByCaseIdAndCategory(caseId, category);
    }

    /**
     * Buscar documentos por texto (flexible)
     */
    @Transactional(readOnly = true)
    public List<Document> searchDocuments(Long caseId, String searchTerm) {
        if (caseId == null) {
            return documentRepository.searchAllDocuments(searchTerm);
        }
        return documentRepository.searchInCaseDocuments(caseId, searchTerm);
    }

    /**
     * Buscar documentos por tipo de archivo (flexible)
     */
    @Transactional(readOnly = true)
    public List<Document> getDocumentsByFileType(Long caseId, String fileType) {
        if (caseId == null) {
            return documentRepository.findByFileType(fileType);
        }
        return documentRepository.findByCaseIdAndFileType(caseId, fileType);
    }

    /**
     * Obtener documentos sin caso asignado
     */
    @Transactional(readOnly = true)
    public List<Document> getOrphanedDocuments() {
        return documentRepository.findDocumentsWithoutCase();
    }

    /**
     * Obtener documento por ID
     */
    @Transactional(readOnly = true)
    public Document getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + documentId));
    }

    // ==========================================
    // MÉTODOS DE GESTIÓN
    // ==========================================

    /**
     * Actualizar metadatos de documento
     */
    public Document updateDocumentMetadata(Long documentId, String description,
                                           DocumentCategory category) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + documentId));

        if (description != null) {
            document.setDescription(description);
        }

        if (category != null) {
            document.setCategory(category);
        }

        return documentRepository.save(document);
    }

    /**
     * Asignar documento a un caso
     */
    public Document assignDocumentToCase(Long documentId, Long caseId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + documentId));

        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Caso legal no encontrado: " + caseId));

        document.setLegalCase(legalCase);
        return documentRepository.save(document);
    }

    /**
     * Desasignar documento de un caso
     */
    public Document unassignDocumentFromCase(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + documentId));

        document.setLegalCase(null);
        return documentRepository.save(document);
    }

    /**
     * Eliminar documento (soft delete)
     */
    public void deleteDocument(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + documentId));

        document.softDelete();
        documentRepository.save(document);

        log.info("Documento eliminado (soft delete): ID={}, Usuario={}", documentId, userId);
    }

    /**
     * Obtener estadísticas de documentos (flexible)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDocumentStatistics(Long caseId) {
        Map<String, Object> stats = new HashMap<>();

        if (caseId == null) {
            // Estadísticas globales
            stats.put("totalDocuments", documentRepository.countAllActive());
            stats.put("totalSize", documentRepository.getTotalFileSize());
            stats.put("byCategory", documentRepository.countByCategory());
            stats.put("byFileType", documentRepository.countByFileType());
            stats.put("orphanedDocuments", documentRepository.findDocumentsWithoutCase().size());
        } else {
            // Estadísticas por caso
            stats.put("totalDocuments", documentRepository.countActiveByCaseId(caseId));
            stats.put("totalSize", documentRepository.getTotalFileSizeByCaseId(caseId));
            stats.put("byCategory", documentRepository.countByCategoryAndCaseId(caseId));
            stats.put("byFileType", documentRepository.countByFileTypeAndCaseId(caseId));
        }

        return stats;
    }

    // ==========================================
    // MÉTODOS PRIVADOS DE UTILIDAD
    // ==========================================

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("El archivo está vacío");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("El archivo excede el tamaño máximo permitido: " +
                    (maxFileSize / 1024 / 1024) + "MB");
        }

        String extension = getFileExtension(file.getOriginalFilename()).toUpperCase();
        List<String> allowedTypesList = Arrays.asList(allowedTypes.split(","));

        if (!allowedTypesList.contains(extension)) {
            throw new RuntimeException("Tipo de archivo no permitido: " + extension +
                    ". Tipos permitidos: " + String.join(", ", allowedTypesList));
        }
    }

    private void validateCaseCapacity(Long caseId) {
        if (caseId == null) return; // No validar si no hay caso

        long currentCount = documentRepository.countActiveByCaseId(caseId);
        if (currentCount >= maxFilesPerCase) {
            throw new RuntimeException("Se ha alcanzado el límite máximo de archivos por caso: " + maxFilesPerCase);
        }
    }

    private String generateUniqueBlobName(String originalFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFileName);
        return timestamp + "_" + uuid + "." + extension;
    }

    private String generateSafeFileName(String originalFileName) {
        return originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }

    private String getContainerForFileType(String extension) {
        if (Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension.toLowerCase())) {
            return imagesContainer;
        }
        return documentsContainer;
    }

    private String calculateFileHash(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculando hash del archivo", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return null;

        String[] headerNames = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}