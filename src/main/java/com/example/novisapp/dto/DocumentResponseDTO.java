package com.example.novisapp.dto;

import com.example.novisapp.entity.DocumentCategory;
import com.example.novisapp.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO simple para Document sin relaciones JPA problemáticas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDTO {

    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private String fileSizeFormatted;
    private DocumentCategory category;
    private String description;
    private Integer version;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long accessCount;

    // Solo IDs, no objetos completos
    private Long caseId;
    private String caseName;
    private Long uploadedById;
    private String uploadedByName;

    /**
     * Convertir Document a DTO seguro
     */
    public static DocumentResponseDTO fromDocument(Document document) {
        if (document == null) return null;

        return DocumentResponseDTO.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .fileType(document.getFileType())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .fileSizeFormatted(document.getFileSizeFormatted())
                .category(document.getCategory())
                .description(document.getDescription())
                .version(document.getVersion())
                .isActive(document.getIsActive())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .accessCount(document.getAccessCount())
                // Solo IDs para evitar lazy loading
                .caseId(document.getLegalCase() != null ? document.getLegalCase().getId() : null)
                .caseName(document.getLegalCase() != null ? document.getLegalCase().getTitle() : null)
                .uploadedById(document.getUploadedBy() != null ? document.getUploadedBy().getId() : null)
                .uploadedByName(document.getUploadedBy() != null ? document.getUploadedBy().getFullName() : "Unknown")
                .build();
    }
}