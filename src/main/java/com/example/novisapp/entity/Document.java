package com.example.novisapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad Document para gestión documental avanzada
 * Soporte para Azure Blob Storage y metadatos completos
 * VERSIÓN CORREGIDA - Sin problemas de NULL
 */
@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 50)
    private String fileType; // PDF, DOCX, XLSX, etc.

    @Column(nullable = false, length = 100)
    private String mimeType; // application/pdf, etc.

    @Column(nullable = false)
    private Long fileSize; // en bytes

    @Column(nullable = false, length = 500)
    private String blobUrl; // URL en Azure Blob Storage

    @Column(nullable = false, length = 100)
    private String containerName; // Container en Azure Blob

    @Column(nullable = false, length = 255)
    private String blobName; // Nombre único del blob

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentCategory category;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @Column(length = 64)
    private String fileHash; // SHA-256 para integridad

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPublic = false;

    // Relaciones - usando clases del mismo paquete
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_case_id", nullable = false)
    private LegalCase legalCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    // Timestamps automáticos
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    // Metadatos adicionales para auditoría
    @Column(length = 45)
    private String uploadIpAddress;

    @Column(length = 500)
    private String userAgent;

    @Column
    private LocalDateTime lastAccessedAt;

    @Builder.Default
    @Column(nullable = false)
    private Long accessCount = 0L;

    // Métodos de utilidad
    public String getFileExtension() {
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }

    public String getFileSizeFormatted() {
        if (fileSize == null) return "0 B";

        String[] units = {"B", "KB", "MB", "GB"};
        double size = fileSize.doubleValue();
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }

    public boolean isPdf() {
        return "PDF".equalsIgnoreCase(fileType);
    }

    public boolean isImage() {
        return fileType != null && (
                fileType.equalsIgnoreCase("JPG") ||
                        fileType.equalsIgnoreCase("JPEG") ||
                        fileType.equalsIgnoreCase("PNG") ||
                        fileType.equalsIgnoreCase("GIF")
        );
    }

    public boolean isOfficeDocument() {
        return fileType != null && (
                fileType.equalsIgnoreCase("DOCX") ||
                        fileType.equalsIgnoreCase("DOC") ||
                        fileType.equalsIgnoreCase("XLSX") ||
                        fileType.equalsIgnoreCase("XLS") ||
                        fileType.equalsIgnoreCase("PPTX")
        );
    }

    // Método para incrementar contador de acceso
    public void incrementAccessCount() {
        if (this.accessCount == null) {
            this.accessCount = 0L;
        }
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    // Método para soft delete
    public void softDelete() {
        this.isActive = false;
        this.deletedAt = LocalDateTime.now();
    }

    // Método para inicializar valores por defecto (si es necesario)
    @PrePersist
    public void prePersist() {
        if (this.accessCount == null) {
            this.accessCount = 0L;
        }
        if (this.version == null) {
            this.version = 1;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isPublic == null) {
            this.isPublic = false;
        }
    }
}