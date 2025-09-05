package com.example.novisapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para perfil de abogados
 * Compatible con LawyerProfile del frontend TypeScript
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LawyerProfileDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Boolean isActive;

    // Información profesional específica
    private String licenseNumber;
    private String licenseState;
    private String licenseExpiry;
    private String barAssociation;
    private Integer yearsExperience;

    // Especializaciones
    private List<String> specializations;

    // Métricas profesionales
    private Integer casesCount;
    private Integer activeCasesCount;
    private Integer completedCasesCount;
    private Double successRate;
    private Double averageCaseDuration;

    // Disponibilidad y tarifas
    private Boolean isAvailableForNewCases;
    private Double hourlyRate;
    private Integer maxCasesAtOnce;

    // Metadatos
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}