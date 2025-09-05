package com.example.novisapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_lawyer_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseLawyerAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_case_id", nullable = false)
    private LegalCase legalCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // CORREGIDO: usar user para coincidir con user_id

    // CORREGIDO: Mapear enum a String en la BD
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(length = 100)
    private String role = "ASSOCIATE";

    @Column(name = "assigned_specialty")
    private String assignedSpecialty;

    @Column(name = "assigned_date")
    private LocalDateTime assignedDate;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "estimated_hours")
    private Integer estimatedHours = 0;

    @Column(name = "actual_hours")
    private Integer actualHours = 0;

    @Column(name = "assignment_notes", length = 500)
    private String assignmentNotes;

    @Column(length = 1000)
    private String responsibilities;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    // ============ MÉTODOS UTILITARIOS ============

    public boolean isActive() {
        return status == AssignmentStatus.ACTIVE;
    }

    public boolean isPending() {
        return status == AssignmentStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == AssignmentStatus.COMPLETED;
    }

    public boolean isInactive() {
        return status == AssignmentStatus.INACTIVE;
    }

    public boolean isCancelled() {
        return status == AssignmentStatus.CANCELLED;
    }

    public boolean isWorking() {
        return status.isActive();
    }

    public boolean countsTowardWorkload() {
        return status.countsForWorkload();
    }

    public String getAssignmentReference() {
        if (legalCase != null && user != null) {
            return legalCase.getCaseNumber() + " - " + user.getFullName();
        }
        return "Asignación #" + id;
    }

    public boolean isOvertime() {
        return estimatedHours != null && actualHours != null && actualHours > estimatedHours;
    }

    public Integer getRemainingHours() {
        if (estimatedHours == null || actualHours == null) {
            return null;
        }
        return Math.max(0, estimatedHours - actualHours);
    }

    public boolean hasRole(String targetRole) {
        return role != null && role.equalsIgnoreCase(targetRole);
    }

    public boolean isLeadLawyer() {
        return hasRole("LEAD");
    }

    public boolean isAssociate() {
        return hasRole("ASSOCIATE");
    }

    public boolean isConsultant() {
        return hasRole("CONSULTANT");
    }

    // Métodos para transiciones de estado
    public boolean canTransitionTo(AssignmentStatus newStatus) {
        if (newStatus == null) return false;

        return switch (status) {
            case PENDING -> newStatus == AssignmentStatus.ACTIVE ||
                    newStatus == AssignmentStatus.INACTIVE ||
                    newStatus == AssignmentStatus.CANCELLED;
            case ACTIVE -> newStatus == AssignmentStatus.COMPLETED ||
                    newStatus == AssignmentStatus.INACTIVE ||
                    newStatus == AssignmentStatus.CANCELLED;
            case INACTIVE -> newStatus == AssignmentStatus.ACTIVE ||
                    newStatus == AssignmentStatus.PENDING ||
                    newStatus == AssignmentStatus.CANCELLED;
            case COMPLETED -> newStatus == AssignmentStatus.INACTIVE;
            case CANCELLED -> false;
        };
    }

    public void activate() {
        if (canTransitionTo(AssignmentStatus.ACTIVE)) {
            this.status = AssignmentStatus.ACTIVE;
            if (startDate == null) {
                this.startDate = LocalDateTime.now();
            }
        }
    }

    public void complete() {
        if (canTransitionTo(AssignmentStatus.COMPLETED)) {
            this.status = AssignmentStatus.COMPLETED;
            this.endDate = LocalDateTime.now();
        }
    }

    public void deactivate() {
        if (canTransitionTo(AssignmentStatus.INACTIVE)) {
            this.status = AssignmentStatus.INACTIVE;
            this.endDate = LocalDateTime.now();
        }
    }

    public void cancel() {
        if (canTransitionTo(AssignmentStatus.CANCELLED)) {
            this.status = AssignmentStatus.CANCELLED;
            this.endDate = LocalDateTime.now();
        }
    }

    // MÉTODOS DE COMPATIBILIDAD
    public boolean isValidAssignment() {
        return legalCase != null && user != null && status != null;
    }

    public boolean hasValidTimeRange() {
        if (startDate == null) return true;
        if (endDate == null) return true;
        return !endDate.isBefore(startDate);
    }

    public boolean hasRealisticHours() {
        if (actualHours == null || actualHours == 0) return true;
        if (estimatedHours != null && estimatedHours > 0) {
            return actualHours <= (estimatedHours * 2);
        }
        return true;
    }

    public void markAsCreatedBy(Long userId) {
        this.createdBy = userId;
    }

    public void markAsUpdatedBy(Long userId) {
        this.updatedBy = userId;
    }

    public Long getDurationInDays() {
        if (startDate == null) return null;
        LocalDateTime endReference = endDate != null ? endDate : LocalDateTime.now();
        return java.time.Duration.between(startDate, endReference).toDays();
    }

    public boolean isFinalState() {
        return status.isFinal();
    }

    public boolean canBeModified() {
        return !status.isFinal();
    }

    public Long getEstimatedDurationInDays() {
        if (estimatedHours == null || estimatedHours == 0) return null;
        return (long) Math.ceil(estimatedHours / 8.0);
    }

    public boolean requiresAttention() {
        return isOvertime() ||
                (status == AssignmentStatus.ACTIVE && endDate != null && endDate.isBefore(LocalDateTime.now()));
    }

    public String getDisplayStatus() {
        return status.getDisplayName();
    }

    public String getUIColor() {
        return status.getUIColor();
    }

    public int getPriority() {
        return status.getPriority();
    }

    // Métodos de compatibilidad con código existente
    public User getLawyer() {
        return user; // Para compatibilidad
    }

    public void setLawyer(User user) {
        this.user = user; // Para compatibilidad
    }

    public Boolean getIsActive() {
        return status == AssignmentStatus.ACTIVE;
    }

    public void setIsActive(Boolean active) {
        this.status = active ? AssignmentStatus.ACTIVE : AssignmentStatus.INACTIVE;
    }

    @Override
    public String toString() {
        return String.format("CaseLawyerAssignment{id=%d, case=%s, user=%s, status=%s, role=%s}",
                id,
                legalCase != null ? legalCase.getCaseNumber() : "null",
                user != null ? user.getFullName() : "null",
                status != null ? status.getDisplayName() : "null",
                role);
    }
}