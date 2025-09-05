// =================================================================
// TimeEntry.java
// =================================================================
// Ubicación: src/main/java/com/example/novisapp/entity/TimeEntry.java

package com.example.novisapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * Entidad que representa una entrada de tiempo trabajado en un caso específico
 * Incluye información de duración, descripción, tarifa y estado de facturación
 */
@Entity
@Table(name = "time_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================
    // RELACIONES
    // ========================================

    /**
     * Caso legal al que pertenece esta entrada de tiempo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_case_id", nullable = false)
    private LegalCase legalCase;

    /**
     * Configuración financiera del caso (opcional)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_case_id")
    private FinancialCase financialCase;

    /**
     * Abogado que registró el tiempo trabajado
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_id", nullable = false)
    private User lawyer;

    // ========================================
    // INFORMACIÓN DEL TIEMPO TRABAJADO
    // ========================================

    /**
     * Hora de inicio del trabajo (opcional)
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * Hora de fin del trabajo (opcional)
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * Duración del trabajo en horas (obligatorio)
     * Ej: 1.5 = 1 hora 30 minutos
     */
    @Column(name = "duration", precision = 8, scale = 2, nullable = false)
    private BigDecimal duration;

    /**
     * Fecha en que se realizó el trabajo
     */
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    // ========================================
    // DESCRIPCIÓN DEL TRABAJO
    // ========================================

    /**
     * Descripción detallada del trabajo realizado
     */
    @Column(name = "description", length = 2000, nullable = false)
    private String description;

    /**
     * Categoría de la tarea realizada
     * Ej: Research, Drafting, Court, Meeting, etc.
     */
    @Column(name = "task_category", length = 100)
    private String taskCategory;

    /**
     * Tipo de actividad específica
     * Ej: Legal Research, Document Review, Client Call
     */
    @Column(name = "activity_type", length = 100)
    private String activityType;

    // ========================================
    // INFORMACIÓN FINANCIERA
    // ========================================

    /**
     * Tarifa por hora aplicada a esta entrada
     */
    @Column(name = "hourly_rate", precision = 19, scale = 2, nullable = false)
    private BigDecimal hourlyRate;

    /**
     * Monto total calculado (duration * hourlyRate)
     */
    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /**
     * Indica si esta entrada es facturable al cliente
     */
    @Column(name = "billable", nullable = false)
    private Boolean billable = true;

    /**
     * Indica si ya ha sido incluida en una factura
     */
    @Column(name = "billed", nullable = false)
    private Boolean billed = false;

    /**
     * Descuento aplicado a esta entrada (porcentaje)
     */
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    /**
     * Monto después del descuento
     */
    @Column(name = "discounted_amount", precision = 19, scale = 2)
    private BigDecimal discountedAmount;

    // ========================================
    // ESTADO Y APROBACIONES
    // ========================================

    /**
     * Estado actual de la entrada de tiempo
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TimeEntryStatus status = TimeEntryStatus.DRAFT;

    /**
     * Usuario que aprobó la entrada
     */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    /**
     * Fecha de aprobación
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * Razón de rechazo (si aplica)
     */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /**
     * Fecha de rechazo
     */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    // ========================================
    // INFORMACIÓN ADICIONAL
    // ========================================

    /**
     * ID de la factura donde se incluyó esta entrada
     */
    @Column(name = "invoice_id", length = 100)
    private String invoiceId;

    /**
     * Indica si es visible para el cliente en reportes
     */
    @Column(name = "client_visible", nullable = false)
    private Boolean clientVisible = true;

    /**
     * Notas internas no visibles al cliente
     */
    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    /**
     * Número de revisión (para tracking de cambios)
     */
    @Column(name = "revision_number")
    private Integer revisionNumber = 1;

    /**
     * ID de la entrada original (si es una revisión)
     */
    @Column(name = "original_entry_id")
    private Long originalEntryId;

    // ========================================
    // CAMPOS DE AUDITORÍA
    // ========================================

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // ========================================
    // MÉTODOS DE CÁLCULO
    // ========================================

    /**
     * Calcula el monto total automáticamente
     */
    public void calculateTotalAmount() {
        if (duration != null && hourlyRate != null) {
            this.totalAmount = duration.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);

            // Aplicar descuento si existe
            if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discountAmount = totalAmount.multiply(discountPercentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                this.discountedAmount = totalAmount.subtract(discountAmount);
            } else {
                this.discountedAmount = totalAmount;
            }
        }
    }

    /**
     * Obtiene el monto final a facturar (con descuentos aplicados)
     * @return monto final
     */
    public BigDecimal getFinalAmount() {
        return discountedAmount != null ? discountedAmount : totalAmount;
    }

    /**
     * Calcula el monto del descuento aplicado
     * @return monto descontado
     */
    public BigDecimal getDiscountAmount() {
        if (discountPercentage == null || totalAmount == null) return BigDecimal.ZERO;
        return totalAmount.multiply(discountPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // ========================================
    // MÉTODOS DE FORMATO
    // ========================================

    /**
     * Formatea la duración como texto legible
     * @return duración formateada (ej: "1:30")
     */
    public String getFormattedDuration() {
        if (duration == null) return "0:00";

        int hours = duration.intValue();
        int minutes = (int) ((duration.doubleValue() - hours) * 60);

        return String.format("%d:%02d", hours, minutes);
    }

    /**
     * Convierte la duración a minutos totales
     * @return minutos totales
     */
    public int getTotalMinutes() {
        if (duration == null) return 0;
        return (int) (duration.doubleValue() * 60);
    }

    /**
     * Obtiene descripción completa del trabajo con categoría
     * @return descripción completa
     */
    public String getWorkDescription() {
        StringBuilder desc = new StringBuilder();

        if (taskCategory != null && !taskCategory.trim().isEmpty()) {
            desc.append("[").append(taskCategory).append("] ");
        }

        if (activityType != null && !activityType.trim().isEmpty()) {
            desc.append(activityType).append(": ");
        }

        desc.append(description);

        return desc.toString();
    }

    // ========================================
    // MÉTODOS DE VALIDACIÓN Y ESTADO
    // ========================================

    /**
     * Verifica si la entrada puede ser editada
     * @return true si puede editarse
     */
    public boolean canEdit() {
        return status == TimeEntryStatus.DRAFT || status == TimeEntryStatus.REJECTED;
    }

    /**
     * Verifica si la entrada puede ser aprobada
     * @return true si puede aprobarse
     */
    public boolean canApprove() {
        return status == TimeEntryStatus.SUBMITTED;
    }

    /**
     * Verifica si la entrada puede ser facturada
     * @return true si puede facturarse
     */
    public boolean canBill() {
        return status == TimeEntryStatus.APPROVED && billable && !billed;
    }

    /**
     * Verifica si la entrada puede ser rechazada
     * @return true si puede rechazarse
     */
    public boolean canReject() {
        return status == TimeEntryStatus.SUBMITTED;
    }

    /**
     * Verifica si es tiempo extra (más de 8 horas en el día)
     * @return true si es overtime
     */
    public boolean isOvertime() {
        return duration != null && duration.compareTo(BigDecimal.valueOf(8)) > 0;
    }

    /**
     * Verifica si es una entrada de tiempo mínima facturable
     * @return true si cumple el tiempo mínimo
     */
    public boolean meetsMinimumBillableTime() {
        // Mínimo 6 minutos (0.1 horas) para ser facturable
        return duration != null && duration.compareTo(BigDecimal.valueOf(0.1)) >= 0;
    }

    /**
     * Verifica si la entrada es del día actual
     * @return true si es de hoy
     */
    public boolean isToday() {
        return workDate != null && workDate.equals(LocalDate.now());
    }

    // ========================================
    // MÉTODOS DE ESTADO DE NEGOCIO
    // ========================================

    /**
     * Marca la entrada como enviada para aprobación
     */
    public void submitForApproval() {
        if (canEdit()) {
            this.status = TimeEntryStatus.SUBMITTED;
            calculateTotalAmount();
        }
    }

    /**
     * Aprueba la entrada de tiempo
     * @param approverName nombre del aprobador
     */
    public void approve(String approverName) {
        if (canApprove()) {
            this.status = TimeEntryStatus.APPROVED;
            this.approvedBy = approverName;
            this.approvedAt = LocalDateTime.now();
            this.rejectionReason = null;
        }
    }

    /**
     * Rechaza la entrada de tiempo
     * @param reason razón del rechazo
     */
    public void reject(String reason) {
        if (canReject()) {
            this.status = TimeEntryStatus.REJECTED;
            this.rejectionReason = reason;
            this.rejectedAt = LocalDateTime.now();
            this.approvedBy = null;
            this.approvedAt = null;
        }
    }

    /**
     * Marca la entrada como facturada
     * @param invoiceId ID de la factura
     */
    public void markAsBilled(String invoiceId) {
        if (canBill()) {
            this.billed = true;
            this.invoiceId = invoiceId;
            this.status = TimeEntryStatus.BILLED;
        }
    }

    // ========================================
    // MÉTODOS PARA REPORTES
    // ========================================

    /**
     * Genera resumen de la entrada para reportes
     * @return resumen textual
     */
    public String getSummary() {
        return String.format("%s - %s (%s) - %s horas - $%s - %s",
                workDate,
                legalCase != null ? legalCase.getCaseNumber() : "N/A",
                lawyer != null ? lawyer.getFullName() : "N/A",
                getFormattedDuration(),
                getFinalAmount(),
                status.getDisplayName());
    }

    /**
     * Obtiene información detallada para auditoría
     * @return información de auditoría
     */
    public String getAuditInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Entrada ID: ").append(id).append("\n");
        info.append("Caso: ").append(legalCase != null ? legalCase.getCaseReference() : "N/A").append("\n");
        info.append("Abogado: ").append(lawyer != null ? lawyer.getFullName() : "N/A").append("\n");
        info.append("Fecha: ").append(workDate).append("\n");
        info.append("Duración: ").append(getFormattedDuration()).append("\n");
        info.append("Tarifa: $").append(hourlyRate).append("/hora\n");
        info.append("Monto: $").append(totalAmount).append("\n");

        if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            info.append("Descuento: ").append(discountPercentage).append("% ($").append(getDiscountAmount()).append(")\n");
            info.append("Monto Final: $").append(getFinalAmount()).append("\n");
        }

        info.append("Estado: ").append(status.getDisplayName()).append("\n");
        info.append("Facturable: ").append(billable ? "Sí" : "No").append("\n");
        info.append("Facturado: ").append(billed ? "Sí" : "No").append("\n");
        info.append("Creado: ").append(createdAt).append("\n");

        if (approvedAt != null) {
            info.append("Aprobado: ").append(approvedAt).append(" por ").append(approvedBy).append("\n");
        }

        return info.toString();
    }

    // ========================================
    // MÉTODO toString PERSONALIZADO
    // ========================================

    @Override
    public String toString() {
        return String.format("TimeEntry{id=%d, case='%s', lawyer='%s', date=%s, duration=%s, amount=$%s, status=%s}",
                id,
                legalCase != null ? legalCase.getCaseNumber() : "N/A",
                lawyer != null ? lawyer.getFullName() : "N/A",
                workDate,
                getFormattedDuration(),
                getFinalAmount(),
                status);
    }
}