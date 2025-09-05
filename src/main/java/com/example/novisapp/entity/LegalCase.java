package com.example.novisapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "legal_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String caseNumber; // Número único generado automáticamente

    @Column(nullable = false, length = 300)
    private String title; // Título del caso

    @Column(length = 2000)
    private String description; // Descripción del proyecto legal

    @Column(length = 1000)
    private String justification; // Justificación para la aceptación del caso

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status = CaseStatus.OPEN;

    @Column(length = 100)
    private String caseType = "General"; // MANTENER para compatibilidad

    // NUEVOS CAMPOS para el sistema de asignación inteligente
    @Enumerated(EnumType.STRING)
    @Column(name = "required_specialty")
    private LegalSpecialty requiredSpecialty = LegalSpecialty.CIVIL_LAW;// Especialidad principal requerida

    @Enumerated(EnumType.STRING)
    @Column(name = "operating_country")
    private Country country = Country.MEXICO; // País donde opera el caso

    @Enumerated(EnumType.STRING)
    @Column(name = "case_complexity")
    private CaseComplexity complexity = CaseComplexity.MEDIUM; // Complejidad del caso

    @Column(precision = 19, scale = 2)
    private BigDecimal estimatedValue; // Valor estimado del caso

    @Column(length = 100)
    private String priority = "MEDIUM"; // "LOW", "MEDIUM", "HIGH", "URGENT"

    // Relación con cliente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // CORREGIDO: Usar legal_case_id en lugar de case_id
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "case_lawyer_assignments",
            joinColumns = @JoinColumn(name = "legal_case_id"), // ✅ CORREGIDO
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignedLawyers = new HashSet<>();

    // Abogado principal responsable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_lawyer_id")
    private User primaryLawyer;

    // NUEVAS relaciones con el sistema mejorado
    @OneToMany(mappedBy = "legalCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CaseLawyerAssignment> teamAssignments = new HashSet<>();

    // Fechas importantes
    @Column(name = "expected_completion_date")
    private LocalDateTime expectedCompletionDate;

    @Column(name = "actual_completion_date")
    private LocalDateTime actualCompletionDate;

    @Column(name = "last_client_contact")
    private LocalDateTime lastClientContact;

    // Archivos y documentos
    @Column(length = 500)
    private String offerDocumentPath;

    @Column(name = "document_count")
    private Integer documentCount = 0;

    // Campos de seguimiento
    @Column(name = "post_delivery_follow_up")
    private Boolean postDeliveryFollowUp = false;

    @Column(name = "client_feedback")
    private String clientFeedback;

    @Column(name = "client_feedback_date")
    private LocalDateTime clientFeedbackDate;

    // NUEVOS campos para el workflow avanzado
    @Column(name = "team_assigned")
    private Boolean teamAssigned = false; // Si ya tiene equipo asignado

    @Column(name = "minimum_lawyers_required")
    private Integer minimumLawyersRequired = 2; // Mínimo de abogados

    @Column(name = "maximum_lawyers_allowed")
    private Integer maximumLawyersAllowed = 5; // Máximo de abogados

    @Column(name = "assignment_notes")
    private String assignmentNotes; // Notas especiales para asignación

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Métodos utilitarios EXISTENTES
    public String getCaseReference() {
        return caseNumber + " - " + title;
    }

    public boolean isOverdue() {
        return expectedCompletionDate != null &&
                expectedCompletionDate.isBefore(LocalDateTime.now()) &&
                status != CaseStatus.COMPLETED &&
                status != CaseStatus.CLOSED;
    }

    public boolean hasMinimumLawyers() {
        // Verificar tanto el sistema viejo como el nuevo
        boolean oldSystemCheck = assignedLawyers != null && assignedLawyers.size() >= minimumLawyersRequired;
        boolean newSystemCheck = teamAssignments != null &&
                teamAssignments.stream()
                        .mapToLong(a -> a.getStatus() == AssignmentStatus.ACTIVE ? 1 : 0)
                        .sum() >= minimumLawyersRequired;

        return oldSystemCheck || newSystemCheck;
    }

    public boolean canUploadMoreDocuments() {
        return documentCount == null || documentCount < 200;
    }

    // NUEVOS métodos utilitarios
    public boolean needsTeamAssignment() {
        return !teamAssigned && (teamAssignments == null || teamAssignments.isEmpty());
    }

    public int getActiveTeamSize() {
        if (teamAssignments == null) return 0;
        return (int) teamAssignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE)
                .count();
    }

    public boolean canAddMoreLawyers() {
        return getActiveTeamSize() < maximumLawyersAllowed;
    }

    public String getComplexityDescription() {
        return complexity != null ? complexity.getDisplayName() : "No definida";
    }

    public String getCountryName() {
        return country != null ? country.getDisplayName() : "No definido";
    }

    public String getSpecialtyName() {
        return requiredSpecialty != null ? requiredSpecialty.getDisplayName() : "No definida";
    }

    // Métodos adicionales para mejorar la funcionalidad
    public boolean isActive() {
        return status == CaseStatus.OPEN || status == CaseStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return status == CaseStatus.COMPLETED || status == CaseStatus.CLOSED;
    }

    public boolean isPending() {
        return status == CaseStatus.OPEN;
    }

    public boolean isInProgress() {
        return status == CaseStatus.IN_PROGRESS;
    }

    public boolean isOnHold() {
        return status == CaseStatus.ON_HOLD;
    }

    public boolean isHighPriority() {
        return "HIGH".equals(priority) || "URGENT".equals(priority);
    }

    public boolean isUrgent() {
        return "URGENT".equals(priority);
    }

    public int getTotalAssignedLawyers() {
        return assignedLawyers != null ? assignedLawyers.size() : 0;
    }

    public boolean hasPrimaryLawyer() {
        return primaryLawyer != null;
    }

    public boolean hasDocuments() {
        return documentCount != null && documentCount > 0;
    }

    public boolean requiresFollowUp() {
        return postDeliveryFollowUp != null && postDeliveryFollowUp;
    }

    public boolean hasClientFeedback() {
        return clientFeedback != null && !clientFeedback.trim().isEmpty();
    }

    // Métodos para fechas
    public boolean isDueSoon(int days) {
        if (expectedCompletionDate == null) return false;
        LocalDateTime alertDate = LocalDateTime.now().plusDays(days);
        return expectedCompletionDate.isBefore(alertDate) && !isCompleted();
    }

    public long getDaysUntilDue() {
        if (expectedCompletionDate == null) return -1;
        return java.time.Duration.between(LocalDateTime.now(), expectedCompletionDate).toDays();
    }

    public boolean wasCompletedOnTime() {
        return actualCompletionDate != null &&
                expectedCompletionDate != null &&
                actualCompletionDate.isBefore(expectedCompletionDate);
    }

    // Métodos para validaciones
    public boolean canBeDeleted() {
        return status == CaseStatus.COMPLETED || status == CaseStatus.CLOSED || status == CaseStatus.CANCELLED;
    }

    public boolean canBeEdited() {
        return status != CaseStatus.COMPLETED && status != CaseStatus.CLOSED && status != CaseStatus.CANCELLED;
    }

    public boolean canAssignLawyers() {
        return status != CaseStatus.COMPLETED && status != CaseStatus.CLOSED && status != CaseStatus.CANCELLED;
    }

    public boolean canChangeStatus() {
        return status != CaseStatus.CANCELLED;
    }

    // Método para obtener el progreso estimado basado en el estado
    public int getEstimatedProgress() {
        return switch (status) {
            case OPEN -> 0;
            case IN_PROGRESS -> 50;
            case ON_HOLD -> 25;
            case COMPLETED -> 100;
            case CLOSED -> 100;
            case CANCELLED -> 0;
        };
    }

    // Método para obtener información resumida
    public String getSummary() {
        return String.format("%s - %s (%s) - %s - %d abogado(s) asignado(s)",
                caseNumber,
                title,
                status.getDisplayName(),
                priority,
                getTotalAssignedLawyers());
    }

    // Método toString personalizado
    @Override
    public String toString() {
        return String.format("LegalCase{id=%d, caseNumber='%s', title='%s', status=%s, priority='%s'}",
                id, caseNumber, title, status, priority);
    }
}