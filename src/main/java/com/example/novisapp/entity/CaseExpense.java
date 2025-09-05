// =================================================================
// CaseExpense.java
// =================================================================
// Ubicación: src/main/java/com/example/novisapp/entity/CaseExpense.java

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
 * Entidad que representa los gastos asociados a un caso legal específico
 * Incluye información de categoría, monto, aprobaciones y facturación
 */
@Entity
@Table(name = "case_expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================
    // RELACIONES
    // ========================================

    /**
     * Caso legal al que pertenece este gasto
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
     * Usuario que registró el gasto
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    /**
     * Usuario que aprobó el gasto (si aplica)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedByUser;

    // ========================================
    // INFORMACIÓN DEL GASTO
    // ========================================

    /**
     * Categoría del gasto
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ExpenseCategory category;

    /**
     * Descripción detallada del gasto
     */
    @Column(name = "description", length = 1000, nullable = false)
    private String description;

    /**
     * Monto del gasto
     */
    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * Fecha en que se incurrió el gasto
     */
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    /**
     * Proveedor o comercio donde se realizó el gasto
     */
    @Column(name = "vendor", length = 200)
    private String vendor;

    /**
     * Ubicación donde se realizó el gasto
     */
    @Column(name = "location", length = 200)
    private String location;

    // ========================================
    // INFORMACIÓN DE RECIBO Y DOCUMENTACIÓN
    // ========================================

    /**
     * Número de recibo o factura
     */
    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    /**
     * URL del recibo almacenado en Azure Storage
     */
    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    /**
     * Nombre del archivo del recibo
     */
    @Column(name = "receipt_filename", length = 200)
    private String receiptFilename;

    /**
     * Monto de impuestos incluidos
     */
    @Column(name = "tax_amount", precision = 19, scale = 2)
    private BigDecimal taxAmount;

    /**
     * Porcentaje de impuesto aplicado
     */
    @Column(name = "tax_percentage", precision = 5, scale = 2)
    private BigDecimal taxPercentage;

    /**
     * Moneda del gasto
     */
    @Column(name = "currency", length = 10)
    private String currency = "USD";

    /**
     * Tipo de cambio aplicado (si es moneda extranjera)
     */
    @Column(name = "exchange_rate", precision = 10, scale = 4)
    private BigDecimal exchangeRate;

    // ========================================
    // CONFIGURACIONES DE REEMBOLSO Y FACTURACIÓN
    // ========================================

    /**
     * Indica si el gasto es reembolsable al empleado
     */
    @Column(name = "reimbursable", nullable = false)
    private Boolean reimbursable = true;

    /**
     * Indica si el gasto puede facturarse al cliente
     */
    @Column(name = "billable_to_client", nullable = false)
    private Boolean billableToClient = true;

    /**
     * Porcentaje de markup para facturar al cliente
     */
    @Column(name = "markup_percentage", precision = 5, scale = 2)
    private BigDecimal markupPercentage;

    /**
     * Monto a facturar al cliente (amount + markup)
     */
    @Column(name = "billable_amount", precision = 19, scale = 2)
    private BigDecimal billableAmount;

    /**
     * Monto ya reembolsado al empleado
     */
    @Column(name = "reimbursed_amount", precision = 19, scale = 2)
    private BigDecimal reimbursedAmount;

    // ========================================
    // ESTADO Y APROBACIONES
    // ========================================

    /**
     * Estado actual del gasto
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExpenseStatus status = ExpenseStatus.PENDING;

    /**
     * Nombre del aprobador
     */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    /**
     * Fecha de aprobación
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * Razón de rechazo
     */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /**
     * Fecha de rechazo
     */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    /**
     * Fecha de reembolso
     */
    @Column(name = "reimbursed_at")
    private LocalDateTime reimbursedAt;

    /**
     * Método de reembolso
     */
    @Column(name = "reimbursement_method", length = 50)
    private String reimbursementMethod;

    /**
     * Indica si ya fue incluido en una factura
     */
    @Column(name = "billed", nullable = false)
    private Boolean billed = false;

    /**
     * Fecha de facturación
     */
    @Column(name = "billed_at")
    private LocalDateTime billedAt;

    // ========================================
    // INFORMACIÓN ADICIONAL
    // ========================================

    /**
     * ID de la factura donde se incluyó
     */
    @Column(name = "invoice_id", length = 100)
    private String invoiceId;

    /**
     * Método de pago utilizado
     */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /**
     * Número de tarjeta o cuenta (últimos 4 dígitos)
     */
    @Column(name = "payment_reference", length = 20)
    private String paymentReference;

    /**
     * Indica si es visible para el cliente
     */
    @Column(name = "client_visible", nullable = false)
    private Boolean clientVisible = true;

    /**
     * Notas internas no visibles al cliente
     */
    @Column(name = "internal_notes", length = 1000)
    private String internalNotes;

    /**
     * Notas para el cliente
     */
    @Column(name = "client_notes", length = 500)
    private String clientNotes;

    /**
     * Nivel de prioridad del gasto
     */
    @Column(name = "priority", length = 20)
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT

    /**
     * Indica si requiere documentación adicional
     */
    @Column(name = "requires_additional_docs", nullable = false)
    private Boolean requiresAdditionalDocs = false;

    // ========================================
    // CAMPOS DE AUDITORÍA
    // ========================================

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // ========================================
    // MÉTODOS DE CÁLCULO
    // ========================================

    /**
     * Calcula el monto facturable con markup
     */
    public void calculateBillableAmount() {
        if (amount == null) return;

        BigDecimal baseAmount = amount;

        // Aplicar markup si existe
        if (markupPercentage != null && markupPercentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal markup = baseAmount.multiply(markupPercentage.divide(BigDecimal.valueOf(100)));
            baseAmount = baseAmount.add(markup);
        }

        this.billableAmount = baseAmount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el monto del markup aplicado
     * @return monto del markup
     */
    public BigDecimal getMarkupAmount() {
        if (markupPercentage == null || amount == null) return BigDecimal.ZERO;
        return amount.multiply(markupPercentage.divide(BigDecimal.valueOf(100)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Obtiene el monto final a facturar
     * @return monto final
     */
    public BigDecimal getFinalBillableAmount() {
        return billableAmount != null ? billableAmount : amount;
    }

    /**
     * Calcula el monto neto (sin impuestos)
     * @return monto neto
     */
    public BigDecimal getNetAmount() {
        if (taxAmount != null) {
            return amount.subtract(taxAmount);
        }
        return amount;
    }

    // ========================================
    // MÉTODOS DE VALIDACIÓN Y ESTADO
    // ========================================

    /**
     * Verifica si el gasto puede ser editado
     * @return true si puede editarse
     */
    public boolean canEdit() {
        return status == ExpenseStatus.PENDING || status == ExpenseStatus.REJECTED;
    }

    /**
     * Verifica si el gasto puede ser aprobado
     * @return true si puede aprobarse
     */
    public boolean canApprove() {
        return status == ExpenseStatus.PENDING;
    }

    /**
     * Verifica si el gasto puede ser reembolsado
     * @return true si puede reembolsarse
     */
    public boolean canReimburse() {
        return status == ExpenseStatus.APPROVED && reimbursable && reimbursedAt == null;
    }

    /**
     * Verifica si el gasto puede facturarse al cliente
     * @return true si puede facturarse
     */
    public boolean canBillToClient() {
        return status == ExpenseStatus.APPROVED && billableToClient && !billed;
    }

    /**
     * Verifica si el gasto puede ser rechazado
     * @return true si puede rechazarse
     */
    public boolean canReject() {
        return status == ExpenseStatus.PENDING;
    }

    /**
     * Verifica si tiene recibo adjunto
     * @return true si tiene recibo
     */
    public boolean hasReceipt() {
        return receiptUrl != null && !receiptUrl.trim().isEmpty();
    }

    /**
     * Verifica si es un gasto grande (requiere aprobación especial)
     * @return true si es un gasto significativo
     */
    public boolean isLargeExpense() {
        return amount != null && amount.compareTo(BigDecimal.valueOf(500)) > 0;
    }

    /**
     * Verifica si requiere aprobación por el monto
     * @return true si requiere aprobación
     */
    public boolean requiresApproval() {
        return amount != null && amount.compareTo(BigDecimal.valueOf(100)) > 0;
    }

    /**
     * Verifica si está vencido para reembolso
     * @return true si está vencido
     */
    public boolean isOverdueForReimbursement() {
        if (!canReimburse()) return false;
        return approvedAt != null && approvedAt.isBefore(LocalDateTime.now().minusDays(30));
    }

    // ========================================
    // MÉTODOS DE ESTADO DE NEGOCIO
    // ========================================

    /**
     * Aprueba el gasto
     * @param approverName nombre del aprobador
     */
    public void approve(String approverName) {
        if (canApprove()) {
            this.status = ExpenseStatus.APPROVED;
            this.approvedBy = approverName;
            this.approvedAt = LocalDateTime.now();
            this.rejectionReason = null;
            calculateBillableAmount();
        }
    }

    /**
     * Rechaza el gasto
     * @param reason razón del rechazo
     */
    public void reject(String reason) {
        if (canReject()) {
            this.status = ExpenseStatus.REJECTED;
            this.rejectionReason = reason;
            this.rejectedAt = LocalDateTime.now();
            this.approvedBy = null;
            this.approvedAt = null;
        }
    }

    /**
     * Marca como reembolsado
     * @param amount monto reembolsado
     * @param method método de reembolso
     */
    public void markAsReimbursed(BigDecimal amount, String method) {
        if (canReimburse()) {
            this.status = ExpenseStatus.REIMBURSED;
            this.reimbursedAmount = amount;
            this.reimbursedAt = LocalDateTime.now();
            this.reimbursementMethod = method;
        }
    }

    /**
     * Marca como facturado
     * @param invoiceId ID de la factura
     */
    public void markAsBilled(String invoiceId) {
        if (canBillToClient()) {
            this.billed = true;
            this.billedAt = LocalDateTime.now();
            this.invoiceId = invoiceId;
            this.status = ExpenseStatus.BILLED;
        }
    }

    // ========================================
    // MÉTODOS DE INFORMACIÓN
    // ========================================

    /**
     * Obtiene el nombre de la categoría
     * @return nombre de la categoría
     */
    public String getCategoryDisplayName() {
        return category != null ? category.getDisplayName() : "No definida";
    }

    /**
     * Obtiene información del estado
     * @return información del estado
     */
    public String getStatusInfo() {
        StringBuilder info = new StringBuilder(status.getDisplayName());

        if (status == ExpenseStatus.APPROVED && approvedAt != null) {
            info.append(" (").append(approvedAt.toLocalDate()).append(")");
        }

        if (status == ExpenseStatus.REJECTED && rejectionReason != null) {
            info.append(" - ").append(rejectionReason);
        }

        return info.toString();
    }

    /**
     * Verifica si es un gasto de emergencia
     * @return true si es urgente
     */
    public boolean isUrgent() {
        return "URGENT".equals(priority);
    }

    /**
     * Verifica si es del día actual
     * @return true si es de hoy
     */
    public boolean isToday() {
        return expenseDate != null && expenseDate.equals(LocalDate.now());
    }

    /**
     * Calcula los días desde que se registró el gasto
     * @return días transcurridos
     */
    public long getDaysSinceCreated() {
        return java.time.temporal.ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDate.now());
    }

    // ========================================
    // MÉTODOS PARA REPORTES
    // ========================================

    /**
     * Genera resumen del gasto
     * @return resumen textual
     */
    public String getSummary() {
        return String.format("%s - %s - $%s - %s - %s",
                expenseDate,
                getCategoryDisplayName(),
                amount,
                vendor != null ? vendor : "N/A",
                status.getDisplayName());
    }

    /**
     * Obtiene información detallada para reportes
     * @return información detallada
     */
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== DETALLE DEL GASTO ===\n");
        info.append("ID: ").append(id).append("\n");
        info.append("Caso: ").append(legalCase != null ? legalCase.getCaseReference() : "N/A").append("\n");
        info.append("Categoría: ").append(getCategoryDisplayName()).append("\n");
        info.append("Descripción: ").append(description).append("\n");
        info.append("Monto: $").append(amount).append(" ").append(currency).append("\n");
        info.append("Fecha: ").append(expenseDate).append("\n");

        if (vendor != null) {
            info.append("Proveedor: ").append(vendor).append("\n");
        }

        if (location != null) {
            info.append("Ubicación: ").append(location).append("\n");
        }

        info.append("Estado: ").append(getStatusInfo()).append("\n");
        info.append("Reembolsable: ").append(reimbursable ? "Sí" : "No").append("\n");
        info.append("Facturable: ").append(billableToClient ? "Sí" : "No").append("\n");

        if (billableToClient && billableAmount != null) {
            info.append("Monto a Facturar: $").append(billableAmount).append("\n");
        }

        if (markupPercentage != null && markupPercentage.compareTo(BigDecimal.ZERO) > 0) {
            info.append("Markup: ").append(markupPercentage).append("% ($").append(getMarkupAmount()).append(")\n");
        }

        info.append("Registrado por: ").append(createdByUser != null ? createdByUser.getFullName() : "N/A").append("\n");
        info.append("Fecha de registro: ").append(createdAt.toLocalDate()).append("\n");

        if (hasReceipt()) {
            info.append("Recibo: Disponible\n");
        }

        return info.toString();
    }

    /**
     * Obtiene resumen financiero
     * @return resumen financiero
     */
    public String getFinancialSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Monto Original: $").append(amount);

        if (markupPercentage != null && markupPercentage.compareTo(BigDecimal.ZERO) > 0) {
            summary.append(" + ").append(markupPercentage).append("% markup");
            summary.append(" = $").append(getFinalBillableAmount());
        }

        if (reimbursable && reimbursedAmount != null) {
            summary.append(" | Reembolsado: $").append(reimbursedAmount);
        }

        return summary.toString();
    }

    // ========================================
    // MÉTODO toString PERSONALIZADO
    // ========================================

    @Override
    public String toString() {
        return String.format("CaseExpense{id=%d, case='%s', category=%s, amount=$%s, date=%s, status=%s}",
                id,
                legalCase != null ? legalCase.getCaseNumber() : "N/A",
                category,
                amount,
                expenseDate,
                status);
    }
}