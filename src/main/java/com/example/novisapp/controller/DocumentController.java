package com.example.novisapp.controller;

import com.azure.storage.blob.BlobServiceClient;
import com.example.novisapp.entity.Document;
import com.example.novisapp.entity.DocumentCategory;
import com.example.novisapp.dto.DocumentResponseDTO;
import com.example.novisapp.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión documental avanzada con debugging completo
 * Endpoints para upload, download, gestión y consultas de documentos
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;
    private final BlobServiceClient blobServiceClient; // Para debugging de Azure

    // =================================================
    // ENDPOINTS DE UPLOAD CON DEBUGGING
    // =================================================

    @PostMapping(value = "/cases/{caseId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long caseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "category", required = false) DocumentCategory category,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest request) {

        try {
            // DEBUGGING DETALLADO DEL REQUEST
            log.debug("=== UPLOAD REQUEST DEBUGGING ===");
            log.debug("Case ID: {}", caseId);
            log.debug("User ID: {}", userId);
            log.debug("File name: {}", file.getOriginalFilename());
            log.debug("File size: {} bytes ({} MB)", file.getSize(), file.getSize() / 1024.0 / 1024.0);
            log.debug("File type: {}", file.getContentType());
            log.debug("Category: {}", category);
            log.debug("Description: {}", description);
            log.debug("Request IP: {}", getClientIpAddress(request));
            log.debug("User-Agent: {}", request.getHeader("User-Agent"));
            log.debug("Content-Length header: {}", request.getHeader("Content-Length"));
            log.debug("================================");

            log.info("Starting upload - Case: {}, User: {}, File: {} ({})",
                    caseId, userId, file.getOriginalFilename(), formatFileSize(file.getSize()));

            // Llamar al service
            Document document = documentService.uploadDocument(caseId, userId, file, category, description, request);

            log.debug("Document created successfully:");
            log.debug("- Document ID: {}", document.getId());
            log.debug("- Blob URL: {}", document.getBlobUrl());
            log.debug("- Container: {}", document.getContainerName());
            log.debug("- Blob name: {}", document.getBlobName());

            // Convertir a DTO
            DocumentResponseDTO documentDTO = DocumentResponseDTO.fromDocument(document);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Documento subido exitosamente");
            response.put("document", documentDTO);

            log.info("Upload completed successfully - Document ID: {}", document.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("=== UPLOAD ERROR DEBUGGING ===");
            log.error("Case ID: {}", caseId);
            log.error("User ID: {}", userId);
            log.error("File: {} ({} bytes)", file != null ? file.getOriginalFilename() : "null",
                    file != null ? file.getSize() : 0);
            log.error("Error type: {}", e.getClass().getSimpleName());
            log.error("Error message: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Root cause: {} - {}", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }
            log.error("Full stack trace:", e);
            log.error("===============================");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            errorResponse.put("errorType", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping(value = "/cases/{caseId}/documents/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMultipleDocuments(
            @PathVariable Long caseId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "category", required = false) DocumentCategory defaultCategory,
            HttpServletRequest request) {

        try {
            log.debug("=== MULTIPLE UPLOAD DEBUGGING ===");
            log.debug("Case ID: {}", caseId);
            log.debug("User ID: {}", userId);
            log.debug("Number of files: {}", files.size());
            log.debug("Default category: {}", defaultCategory);

            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                log.debug("File {}: {} ({} bytes, type: {})",
                        i + 1, file.getOriginalFilename(), file.getSize(), file.getContentType());
            }
            log.debug("==================================");

            log.info("Starting multiple upload - Case: {}, User: {}, Files: {}", caseId, userId, files.size());

            List<Document> documents = documentService.uploadMultipleDocuments(caseId, userId, files, defaultCategory, request);

            log.info("Multiple upload completed - {}/{} files successful", documents.size(), files.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Documentos procesados exitosamente");
            response.put("documentsUploaded", documents.size());
            response.put("totalFiles", files.size());
            response.put("documents", documents);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("=== MULTIPLE UPLOAD ERROR ===");
            log.error("Case ID: {}", caseId);
            log.error("User ID: {}", userId);
            log.error("Files count: {}", files != null ? files.size() : 0);
            log.error("Error: {}", e.getMessage(), e);
            log.error("==============================");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // =================================================
    // ENDPOINTS DE DESCARGA
    // =================================================

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long documentId,
            @RequestParam("userId") Long userId) {

        try {
            log.debug("Download request - Document: {}, User: {}", documentId, userId);

            Document document = documentService.getDocumentById(documentId);
            byte[] fileContent = documentService.downloadDocument(documentId, userId);

            log.debug("Download successful - File: {} ({} bytes)",
                    document.getOriginalFileName(), fileContent.length);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(document.getMimeType()));
            headers.setContentDispositionFormData("attachment", document.getOriginalFileName());
            headers.setContentLength(fileContent.length);

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Download error - Document: {}, User: {}, Error: {}", documentId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/documents/{documentId}/download-url")
    public ResponseEntity<?> getDownloadUrl(
            @PathVariable Long documentId,
            @RequestParam(value = "expirationMinutes", defaultValue = "60") int expirationMinutes) {

        try {
            log.debug("Download URL request - Document: {}, Expiration: {} min", documentId, expirationMinutes);

            String downloadUrl = documentService.getDownloadUrl(documentId, expirationMinutes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("downloadUrl", downloadUrl);
            response.put("expirationMinutes", expirationMinutes);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Download URL error - Document: {}, Error: {}", documentId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    // =================================================
    // ENDPOINTS DE CONSULTA
    // =================================================

    @GetMapping("/cases/{caseId}/documents")
    public ResponseEntity<?> getDocumentsByCase(
            @PathVariable Long caseId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        try {
            log.debug("List documents - Case: {}, Page: {}, Size: {}", caseId, page, size);

            if (size > 0) {
                Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
                Page<Document> documentsPage = documentService.getDocumentsByCase(caseId, pageable);

                List<DocumentResponseDTO> documentDTOs = documentsPage.getContent()
                        .stream()
                        .map(DocumentResponseDTO::fromDocument)
                        .collect(java.util.stream.Collectors.toList());

                log.debug("Documents found: {}/{} (page {}/{})",
                        documentDTOs.size(), documentsPage.getTotalElements(),
                        page + 1, documentsPage.getTotalPages());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("documents", documentDTOs);
                response.put("totalElements", documentsPage.getTotalElements());
                response.put("totalPages", documentsPage.getTotalPages());
                response.put("currentPage", page);
                response.put("size", size);

                return ResponseEntity.ok(response);
            } else {
                List<Document> documents = documentService.getDocumentsByCase(caseId);
                List<DocumentResponseDTO> documentDTOs = documents
                        .stream()
                        .map(DocumentResponseDTO::fromDocument)
                        .collect(java.util.stream.Collectors.toList());

                log.debug("All documents found: {}", documentDTOs.size());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("documents", documentDTOs);
                response.put("totalElements", documentDTOs.size());

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("Error listing documents - Case: {}, Error: {}", caseId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/cases/{caseId}/documents/category/{category}")
    public ResponseEntity<?> getDocumentsByCategory(
            @PathVariable Long caseId,
            @PathVariable DocumentCategory category) {

        try {
            log.debug("Search by category - Case: {}, Category: {}", caseId, category);

            List<Document> documents = documentService.getDocumentsByCategory(caseId, category);
            List<DocumentResponseDTO> documentDTOs = documents
                    .stream()
                    .map(DocumentResponseDTO::fromDocument)
                    .collect(java.util.stream.Collectors.toList());

            log.debug("Category search results: {} documents found", documentDTOs.size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documents", documentDTOs);
            response.put("category", category.name());
            response.put("categoryDisplayName", category.getDisplayName());
            response.put("totalElements", documentDTOs.size());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            log.error("Category search error - Case: {}, Category: {}, Error: {}",
                    caseId, category, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("category", category != null ? category.name() : "NULL");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }

    @GetMapping("/cases/{caseId}/documents/search")
    public ResponseEntity<?> searchDocuments(
            @PathVariable Long caseId,
            @RequestParam("q") String searchTerm) {

        try {
            log.debug("Text search - Case: {}, Term: '{}'", caseId, searchTerm);

            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "El término de búsqueda no puede estar vacío");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            List<Document> documents = documentService.searchDocuments(caseId, searchTerm.trim());
            List<DocumentResponseDTO> documentDTOs = documents
                    .stream()
                    .map(DocumentResponseDTO::fromDocument)
                    .collect(java.util.stream.Collectors.toList());

            log.debug("Text search results: {} documents found for '{}'", documentDTOs.size(), searchTerm);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documents", documentDTOs);
            response.put("searchTerm", searchTerm.trim());
            response.put("totalElements", documentDTOs.size());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            log.error("Text search error - Case: {}, Term: '{}', Error: {}",
                    caseId, searchTerm, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("searchTerm", searchTerm);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<?> getDocumentById(@PathVariable Long documentId) {

        try {
            log.debug("Get document by ID: {}", documentId);

            Document document = documentService.getDocumentById(documentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("document", document);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Get document error - ID: {}, Error: {}", documentId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    // =================================================
    // ENDPOINTS DE GESTIÓN
    // =================================================

    @PutMapping("/documents/{documentId}")
    public ResponseEntity<?> updateDocumentMetadata(
            @PathVariable Long documentId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) DocumentCategory category) {

        try {
            log.debug("Update metadata - ID: {}, Description: {}, Category: {}",
                    documentId, description, category);

            Document document = documentService.updateDocumentMetadata(documentId, description, category);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Metadatos actualizados exitosamente");
            response.put("document", document);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Update metadata error - ID: {}, Error: {}", documentId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long documentId,
            @RequestParam("userId") Long userId) {

        try {
            log.debug("Delete document - ID: {}, User: {}", documentId, userId);

            documentService.deleteDocument(documentId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Documento eliminado exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Delete document error - ID: {}, User: {}, Error: {}",
                    documentId, userId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // =================================================
    // ENDPOINTS DE ESTADÍSTICAS
    // =================================================

    @GetMapping("/cases/{caseId}/documents/statistics")
    public ResponseEntity<?> getDocumentStatistics(@PathVariable Long caseId) {

        try {
            log.debug("Get statistics - Case: {}", caseId);

            Map<String, Object> statistics = documentService.getDocumentStatistics(caseId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);
            response.put("caseId", caseId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Statistics error - Case: {}, Error: {}", caseId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/documents/categories")
    public ResponseEntity<?> getAvailableCategories() {

        try {
            log.debug("Get available categories");

            DocumentCategory[] categories = DocumentCategory.values();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("categories", categories);
            response.put("totalCategories", categories.length);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Get categories error: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // =================================================
    // ENDPOINTS DE SALUD Y DEBUGGING
    // =================================================

    @GetMapping("/documents/health")
    public ResponseEntity<?> healthCheck() {

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("service", "DocumentService");
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de debugging para verificar Azure Storage
     */
    @GetMapping("/debug/azure-status")
    public ResponseEntity<?> debugAzureStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            log.debug("=== AZURE STORAGE DEBUG ===");

            status.put("accountName", blobServiceClient.getAccountName());
            status.put("serviceUrl", blobServiceClient.getAccountUrl());

            // Verificar containers
            boolean documentsExists = blobServiceClient.getBlobContainerClient("novis-documents").exists();
            boolean imagesExists = blobServiceClient.getBlobContainerClient("novis-images").exists();

            status.put("documentsContainer", documentsExists);
            status.put("imagesContainer", imagesExists);
            status.put("status", "OK");

            log.debug("Account: {}", blobServiceClient.getAccountName());
            log.debug("Documents container exists: {}", documentsExists);
            log.debug("Images container exists: {}", imagesExists);
            log.debug("==========================");

        } catch (Exception e) {
            log.error("Azure status check failed: {}", e.getMessage(), e);
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
            status.put("errorType", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(status);
    }

    // =================================================
    // MÉTODOS UTILITARIOS PRIVADOS
    // =================================================

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "unknown";

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

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}