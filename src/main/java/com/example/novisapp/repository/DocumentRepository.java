package com.example.novisapp.repository;

import com.example.novisapp.entity.DocumentCategory;
import com.example.novisapp.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestión avanzada de documentos
 * Sistema de gestión documental con Azure Blob Storage
 * Estructura de paquete plano
 * VERSIÓN ACTUALIZADA - Con consultas flexibles para casos NULL
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // ============================================
    // CONSULTAS GENERALES (SIN FILTRO DE CASO)
    // ============================================

    /**
     * Buscar TODOS los documentos activos (sin filtrar por caso)
     */
    @Query("SELECT d FROM Document d WHERE d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findAllActive();

    /**
     * Buscar TODOS los documentos activos con paginación
     */
    @Query("SELECT d FROM Document d WHERE d.isActive = true ORDER BY d.createdAt DESC")
    Page<Document> findAllActive(Pageable pageable);

    /**
     * Contar TODOS los documentos activos
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.isActive = true")
    long countAllActive();

    /**
     * Buscar documentos sin caso asignado
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase IS NULL AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findDocumentsWithoutCase();

    // ============================================
    // CONSULTAS BÁSICAS POR CASO (ORIGINALES + FLEXIBLES)
    // ============================================

    /**
     * Buscar documentos activos por caso
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findActiveByCaseId(@Param("caseId") Long caseId);

    /**
     * Buscar documentos por caso con paginación
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.isActive = true ORDER BY d.createdAt DESC")
    Page<Document> findActiveByCaseId(@Param("caseId") Long caseId, Pageable pageable);

    /**
     * Buscar documentos por ID de caso (permitiendo NULL) - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE (d.legalCase.id = :caseId OR d.legalCase IS NULL) AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByCaseIdOrNull(@Param("caseId") Long caseId);

    /**
     * Buscar documentos por caso específico o sin caso - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE (:caseId IS NULL OR d.legalCase.id = :caseId OR d.legalCase IS NULL) AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByCaseIdFlexible(@Param("caseId") Long caseId);

    /**
     * Contar documentos activos por caso
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.legalCase.id = :caseId AND d.isActive = true")
    long countActiveByCaseId(@Param("caseId") Long caseId);

    // ============================================
    // CONSULTAS POR CATEGORÍA
    // ============================================

    /**
     * Buscar documentos por caso y categoría
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.category = :category AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByCaseIdAndCategory(@Param("caseId") Long caseId, @Param("category") DocumentCategory category);

    /**
     * Buscar documentos por categoría (sin filtro de caso) - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.category = :category AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByCategory(@Param("category") DocumentCategory category);

    /**
     * Buscar documentos por múltiples categorías
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.category IN :categories AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByCaseIdAndCategoryIn(@Param("caseId") Long caseId, @Param("categories") List<DocumentCategory> categories);

    /**
     * Buscar documentos por múltiples categorías (sin filtro de caso) - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.category IN :categories AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByCategoryIn(@Param("categories") List<DocumentCategory> categories);

    // ============================================
    // CONSULTAS POR USUARIO
    // ============================================

    /**
     * Buscar documentos subidos por usuario
     */
    @Query("SELECT d FROM Document d WHERE d.uploadedBy.id = :userId AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByUploadedById(@Param("userId") Long userId);

    /**
     * Buscar documentos por caso y usuario
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.uploadedBy.id = :userId AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByCaseIdAndUploadedById(@Param("caseId") Long caseId, @Param("userId") Long userId);

    /**
     * Buscar documentos por usuario (flexible con caso) - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.uploadedBy.id = :userId AND (:caseId IS NULL OR d.legalCase.id = :caseId OR d.legalCase IS NULL) AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByUploadedByIdFlexible(@Param("userId") Long userId, @Param("caseId") Long caseId);

    // ============================================
    // CONSULTAS POR TIPO DE ARCHIVO
    // ============================================

    /**
     * Buscar documentos por tipo de archivo
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.fileType = :fileType AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByCaseIdAndFileType(@Param("caseId") Long caseId, @Param("fileType") String fileType);

    /**
     * Buscar documentos por tipo de archivo (sin filtro de caso) - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.fileType = :fileType AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByFileType(@Param("fileType") String fileType);

    /**
     * Buscar imágenes por caso
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.fileType IN ('JPG', 'JPEG', 'PNG', 'GIF') AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findImagesByCaseId(@Param("caseId") Long caseId);

    /**
     * Buscar todas las imágenes - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.fileType IN ('JPG', 'JPEG', 'PNG', 'GIF') AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findAllImages();

    /**
     * Buscar PDFs por caso
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.fileType = 'PDF' AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findPdfsByCaseId(@Param("caseId") Long caseId);

    /**
     * Buscar todos los PDFs - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.fileType = 'PDF' AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findAllPdfs();

    // ============================================
    // CONSULTAS POR FECHAS
    // ============================================

    /**
     * Buscar documentos por rango de fechas
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.createdAt BETWEEN :startDate AND :endDate AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByCaseIdAndDateRange(@Param("caseId") Long caseId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Buscar documentos por rango de fechas (sin filtro de caso) - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.createdAt BETWEEN :startDate AND :endDate AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Buscar documentos recientes (últimos N días)
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.createdAt >= :sinceDate AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findRecentByCaseId(@Param("caseId") Long caseId, @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Buscar documentos recientes (sin filtro de caso) - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.createdAt >= :sinceDate AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findRecentDocuments(@Param("sinceDate") LocalDateTime sinceDate);

    // ============================================
    // BÚSQUEDAS AVANZADAS
    // ============================================

    /**
     * Búsqueda por nombre de archivo (LIKE)
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND LOWER(d.originalFileName) LIKE LOWER(CONCAT('%', :fileName, '%')) AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByCaseIdAndFileNameContaining(@Param("caseId") Long caseId, @Param("fileName") String fileName);

    /**
     * Búsqueda por nombre de archivo (sin filtro de caso) - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE LOWER(d.originalFileName) LIKE LOWER(CONCAT('%', :fileName, '%')) AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByFileNameContaining(@Param("fileName") String fileName);

    /**
     * Búsqueda por descripción
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND LOWER(d.description) LIKE LOWER(CONCAT('%', :description, '%')) AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByCaseIdAndDescriptionContaining(@Param("caseId") Long caseId, @Param("description") String description);

    /**
     * Búsqueda por descripción (sin filtro de caso) - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE LOWER(d.description) LIKE LOWER(CONCAT('%', :description, '%')) AND d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> findByDescriptionContaining(@Param("description") String description);

    /**
     * Búsqueda full-text (nombre + descripción)
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND " +
            "(LOWER(d.originalFileName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> searchInCaseDocuments(@Param("caseId") Long caseId, @Param("searchTerm") String searchTerm);

    /**
     * Búsqueda full-text global (sin filtro de caso) - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE " +
            "(LOWER(d.originalFileName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "d.isActive = true ORDER BY d.createdAt DESC")
    List<Document> searchAllDocuments(@Param("searchTerm") String searchTerm);

    // ============================================
    // VALIDACIONES Y DUPLICADOS
    // ============================================

    /**
     * Buscar documento por hash (para detectar duplicados)
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.fileHash = :fileHash AND d.isActive = true")
    Optional<Document> findByCaseIdAndFileHash(@Param("caseId") Long caseId, @Param("fileHash") String fileHash);

    /**
     * Buscar documento por hash global - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.fileHash = :fileHash AND d.isActive = true")
    Optional<Document> findByFileHash(@Param("fileHash") String fileHash);

    /**
     * Buscar documento por nombre exacto en caso
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.originalFileName = :fileName AND d.isActive = true")
    Optional<Document> findByCaseIdAndExactFileName(@Param("caseId") Long caseId, @Param("fileName") String fileName);

    /**
     * Buscar documento por nombre exacto global - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.originalFileName = :fileName AND d.isActive = true")
    Optional<Document> findByExactFileName(@Param("fileName") String fileName);

    /**
     * Verificar si existe documento con mismo blob name
     */
    @Query("SELECT d FROM Document d WHERE d.blobName = :blobName AND d.isActive = true")
    Optional<Document> findByBlobName(@Param("blobName") String blobName);

    // ============================================
    // ESTADÍSTICAS Y MÉTRICAS
    // ============================================

    /**
     * Obtener tamaño total de documentos por caso
     */
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.legalCase.id = :caseId AND d.isActive = true")
    Long getTotalFileSizeByCaseId(@Param("caseId") Long caseId);

    /**
     * Obtener tamaño total de TODOS los documentos - NUEVO
     */
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.isActive = true")
    Long getTotalFileSize();

    /**
     * Contar documentos por categoría
     */
    @Query("SELECT d.category, COUNT(d) FROM Document d WHERE d.legalCase.id = :caseId AND d.isActive = true GROUP BY d.category")
    List<Object[]> countByCategoryAndCaseId(@Param("caseId") Long caseId);

    /**
     * Contar documentos por categoría global - NUEVO
     */
    @Query("SELECT d.category, COUNT(d) FROM Document d WHERE d.isActive = true GROUP BY d.category")
    List<Object[]> countByCategory();

    /**
     * Contar documentos por tipo de archivo
     */
    @Query("SELECT d.fileType, COUNT(d) FROM Document d WHERE d.legalCase.id = :caseId AND d.isActive = true GROUP BY d.fileType")
    List<Object[]> countByFileTypeAndCaseId(@Param("caseId") Long caseId);

    /**
     * Contar documentos por tipo de archivo global - NUEVO
     */
    @Query("SELECT d.fileType, COUNT(d) FROM Document d WHERE d.isActive = true GROUP BY d.fileType")
    List<Object[]> countByFileType();

    /**
     * Documentos más accedidos
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.isActive = true ORDER BY d.accessCount DESC")
    List<Document> findMostAccessedByCaseId(@Param("caseId") Long caseId, Pageable pageable);

    /**
     * Documentos más accedidos globalmente - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.isActive = true ORDER BY d.accessCount DESC")
    List<Document> findMostAccessed(Pageable pageable);

    // ============================================
    // AUDITORÍA Y LIMPIEZA
    // ============================================

    /**
     * Buscar documentos eliminados (soft delete)
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase.id = :caseId AND d.isActive = false ORDER BY d.deletedAt DESC")
    List<Document> findDeletedByCaseId(@Param("caseId") Long caseId);

    /**
     * Buscar todos los documentos eliminados - NUEVO
     */
    @Query("SELECT d FROM Document d WHERE d.isActive = false ORDER BY d.deletedAt DESC")
    List<Document> findAllDeleted();

    /**
     * Buscar documentos antiguos para limpieza
     */
    @Query("SELECT d FROM Document d WHERE d.createdAt < :cutoffDate AND d.isActive = false")
    List<Document> findOldDeletedDocuments(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Buscar documentos huérfanos (sin caso válido)
     */
    @Query("SELECT d FROM Document d WHERE d.legalCase IS NULL OR d.legalCase.id NOT IN (SELECT c.id FROM LegalCase c)")
    List<Document> findOrphanedDocuments();

    // ============================================
    // MÉTODOS DE CONVENIENCIA PARA EL FRONTEND
    // ============================================

    /**
     * Método de conveniencia para obtener documentos con manejo flexible de casos
     */
    default List<Document> findDocumentsFlexible(Long caseId) {
        if (caseId == null) {
            return findAllActive();
        } else {
            return findByCaseIdFlexible(caseId);
        }
    }

    /**
     * Método para buscar documentos con o sin caso específico
     */
    default List<Document> findDocumentsForDisplay(Long caseId, boolean includeOrphans) {
        if (caseId == null) {
            return findAllActive();
        } else if (includeOrphans) {
            return findByCaseIdOrNull(caseId);
        } else {
            return findActiveByCaseId(caseId);
        }
    }
}