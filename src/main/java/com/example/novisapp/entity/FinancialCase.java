// =================================================================
// FinancialCase.java
// =================================================================
// Ubicación: src/main/java/com/example/novisapp/entity/FinancialCase.java

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
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que maneja la configuración financiera de los casos legales
 * Incluye tarifas, presupuestos, facturación y totales calculados
 */
@Entity
@Table(name = "financial_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================
    // RELACIÓN CON CASO LEGAL
    // ========================================

    /**
     * Relación uno a uno con el caso legal
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_case_id", nullable = false, unique = true)
    private LegalCase legalCase;

    // ========================================
    // CONFIGURACIÓN DE FACTURACIÓN
    // ========================================

    /**
     * Tipo de facturación para este caso
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type", nullable = false)
    private BillingType billingType = BillingType.HOURLY;

    /**
     * Tarifa por hora estándar
     */
    @Column(name = "hourly_rate", precision = 19, scale = 2)
    private BigDecimal hourlyRate;

    /**
     * Tarifa fija total (para casos de precio fijo)
     */
    @Column(name = "fixed_fee", precision = 19, scale = 2)
    private BigDecimal fixedFee;

    /**
     * Porcentaje de contingencia (para casos de contingencia)
     */
    @Column(name = "contingency_percentage", precision = 5, scale = 2)
    private BigDecimal contingencyPercentage;

    /**
     * Monto del retainer (pago adelantado)
     */
    @Column(name = "retainer_amount", precision = 19, scale = 2)
    private BigDecimal retainerAmount;

    // ========================================
    // ESTIMACIONES Y LÍMITES
    // ========================================

    /**
     * Horas estimadas para completar el caso
     */
    @Column(name = "estimated_hours", precision = 8, scale = 2)
    private BigDecimal estimatedHours;

    /**
     * Límite de presupuesto total
     */
    @Column(name = "budget_limit", precision = 19, scale = 2)
    private BigDecimal budgetLimit;

    /**
     * Límite para gastos
     */
    @Column(name = "expense_limit", precision = 19, scale = 2)
    private BigDecimal expenseLimit;

    // ========================================
    // TOTALES CALCULADOS (SE ACTUALIZAN AUTOMÁTICAMENTE)
    // ========================================

    /**
     * Horas reales trabajadas (calculado automáticamente)
     */
    @Column(name = "actual_hours", precision = 8, scale = 2)
    private BigDecimal actualHours = BigDecimal.ZERO;

    /**
     * Ingresos totales por tiempo trabajado
     */
    @Column(name = "total_time_revenue", precision = 19, scale = 2)
    private BigDecimal totalTimeRevenue = BigDecimal.ZERO;

    /**
     * Total de gastos del caso
     */
    @Column(name = "total_expenses", precision = 19, scale = 2)
    private BigDecimal totalExpenses = BigDecimal.ZERO;

    /**
     * Ingresos totales del caso (tiempo + tarifas fijas)
     */
    @Column(name = "total_revenue", precision = 19, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    /**
     * Margen de ganancia como porcentaje
     */
    @Column(name = "profit_margin", precision = 5, scale = 2)
    private BigDecimal profitMargin = BigDecimal.ZERO;

    // ========================================
    // CONFIGURACIONES ADICIONALES
    // ========================================

    /**
     * Permite override de tarifas por entrada individual
     */
    @Column(name = "billable_rate_override")
    private Boolean billableRateOverride = false;

    /**
     * Facturación automática habilitada
     */
    @Column(name = "auto_invoice")
    private Boolean autoInvoice = false;

    /**
     * Requiere aprobación del cliente para gastos
     */
    @Column(name = "client_approval_required")
    private Boolean clientApprovalRequired = false;

    /**
     * Caso pre-pagado (retainer consumible)
     */
    @Column(name = "prepaid_case")
    private Boolean prepaidCase = false;

    // ========================================
    // NOTAS Y OBSERVACIONES
    // ========================================

    /**
     * Notas sobre la configuración de facturación
     */
    @Column(name = "billing_notes", length = 1000)
    private String billingNotes;

    /**
     * Términos de pago (ej: "Net 30", "Due on receipt")
     */
    @Column(name = "payment_terms", length = 500)
    private String paymentTerms = "Net 30";

    /**
     * Moneda utilizada
     */
    @Column(name = "currency", length = 10)
    private String currency = "USD";

    // ========================================
    // FECHAS IMPORTANTES
    // ========================================

    /**
     * Fecha de inicio de facturación
     */
    @Column(name = "billing_start_date")
    private LocalDateTime billingStartDate;

    /**
     * Fecha de última facturación
     */
    @Column(name = "last_billing_date")
    private LocalDateTime lastBillingDate;

    /**
     * Fecha programada para próxima facturación
     */
    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    // ========================================
    // RELACIONES CON ENTRADAS DE TIEMPO Y GASTOS
    // ========================================

    /**
     * Entradas de tiempo asociadas a este caso financiero
     */
    @OneToMany(mappedBy = "financialCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<TimeEntry> timeEntries = new HashSet<>();

    /**
     * Gastos asociados a este caso financiero
     */
    @OneToMany(mappedBy = "financialCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CaseExpense> expenses = new HashSet<>();

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
    // MÉTODOS DE CÁLCULO FINANCIERO
    // ========================================

    /**
     * Calcula la ganancia neta (ingresos - gastos)
     * @return ganancia neta
     */
    public BigDecimal getNetProfit() {
        if (totalRevenue == null || totalExpenses == null) return BigDecimal.ZERO;
        return totalRevenue.subtract(totalExpenses);
    }

    /**
     * Calcula el margen de ganancia como porcentaje
     * @return margen de ganancia (0-100)
     */
    public BigDecimal calculateProfitMargin() {
        if (totalRevenue == null || totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal profit = getNetProfit();
        return profit.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calcula el valor por hora efectivo del caso
     * @return valor por hora efectivo
     */
    public BigDecimal getEffectiveHourlyRate() {
        if (actualHours == null || actualHours.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalRevenue.divide(actualHours, 2, RoundingMode.HALF_UP);
    }

    // ========================================
    // MÉTODOS DE VALIDACIÓN Y ESTADO
    // ========================================

    /**
     * Verifica si el caso está sobre presupuesto
     * @return true si excede el límite de presupuesto
     */
    public boolean isOverBudget() {
        return budgetLimit != null && totalRevenue != null &&
                totalRevenue.compareTo(budgetLimit) > 0;
    }

    /**
     * Verifica si los gastos exceden el límite
     * @return true si excede el límite de gastos
     */
    public boolean isOverExpenseLimit() {
        return expenseLimit != null && totalExpenses != null &&
                totalExpenses.compareTo(expenseLimit) > 0;
    }

    /**
     * Verifica si las horas trabajadas exceden lo estimado
     * @return true si excede las horas estimadas
     */
    public boolean isOverEstimatedHours() {
        return estimatedHours != null && actualHours != null &&
                actualHours.compareTo(estimatedHours) > 0;
    }

    /**
     * Verifica si el caso está listo para facturar
     * @return true si puede facturarse
     */
    public boolean canBill() {
        return totalTimeRevenue != null && totalTimeRevenue.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Verifica si el retainer ha sido consumido
     * @return true si el retainer está agotado
     */
    public boolean isRetainerDepleted() {
        if (!prepaidCase || retainerAmount == null) return false;
        return totalRevenue.compareTo(retainerAmount) >= 0;
    }

    // ========================================
    // MÉTODOS DE INFORMACIÓN Y REFERENCIA
    // ========================================

    /**
     * Genera una referencia única para facturación
     * @return referencia de facturación
     */
    public String getBillingReference() {
        return legalCase != null ? legalCase.getCaseNumber() + "-FIN" : "N/A";
    }

    /**
     * Obtiene el nombre del cliente
     * @return nombre del cliente
     */
    public String getClientName() {
        return legalCase != null && legalCase.getClient() != null
                ? legalCase.getClient().getName() : "N/A";
    }

    /**
     * Obtiene el porcentaje de presupuesto utilizado
     * @return porcentaje utilizado (0-100)
     */
    public BigDecimal getBudgetUtilizationPercentage() {
        if (budgetLimit == null || budgetLimit.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalRevenue.divide(budgetLimit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Obtiene el porcentaje de horas utilizadas
     * @return porcentaje de horas utilizadas (0-100)
     */
    public BigDecimal getHoursUtilizationPercentage() {
        if (estimatedHours == null || estimatedHours.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return actualHours.divide(estimatedHours, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // ========================================
    // MÉTODOS DE ALERTAS Y NOTIFICACIONES
    // ========================================

    /**
     * Verifica si necesita alerta por presupuesto (>80% utilizado)
     * @return true si necesita alerta
     */
    public boolean needsBudgetAlert() {
        BigDecimal utilization = getBudgetUtilizationPercentage();
        return utilization.compareTo(BigDecimal.valueOf(80)) > 0;
    }

    /**
     * Verifica si necesita alerta por horas (>90% utilizadas)
     * @return true si necesita alerta
     */
    public boolean needsHoursAlert() {
        BigDecimal utilization = getHoursUtilizationPercentage();
        return utilization.compareTo(BigDecimal.valueOf(90)) > 0;
    }

    /**
     * Verifica si necesita alerta por gastos altos
     * @return true si necesita alerta
     */
    public boolean needsExpenseAlert() {
        if (expenseLimit == null || totalExpenses == null) return false;
        BigDecimal utilizationExpenses = totalExpenses.divide(expenseLimit, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return utilizationExpenses.compareTo(BigDecimal.valueOf(75)) > 0;
    }

    // ========================================
    // MÉTODOS DE RENDIMIENTO
    // ========================================

    /**
     * Verifica si el caso es rentable
     * @return true si tiene ganancia positiva
     */
    public boolean isProfitable() {
        return getNetProfit().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Verifica si el caso es altamente rentable (>20% margen)
     * @return true si es muy rentable
     */
    public boolean isHighlyProfitable() {
        return calculateProfitMargin().compareTo(BigDecimal.valueOf(20)) > 0;
    }

    /**
     * Calcula el ROI (Return on Investment)
     * @return ROI como porcentaje
     */
    public BigDecimal calculateROI() {
        if (totalExpenses == null || totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getNetProfit().divide(totalExpenses, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // ========================================
    // MÉTODOS DE ESTADO FINANCIERO
    // ========================================

    /**
     * Obtiene el estado financiero del caso
     * @return estado como texto
     */
    public String getFinancialStatus() {
        if (isOverBudget()) return "SOBRE_PRESUPUESTO";
        if (needsBudgetAlert()) return "ALERTA_PRESUPUESTO";
        if (isProfitable()) return "RENTABLE";
        if (getNetProfit().compareTo(BigDecimal.ZERO) == 0) return "PUNTO_EQUILIBRIO";
        return "PERDIDA";
    }

    /**
     * Verifica si puede facturar automáticamente
     * @return true si puede auto-facturar
     */
    public boolean canAutoInvoice() {
        return autoInvoice && canBill() && !clientApprovalRequired;
    }

    // ========================================
    // MÉTODOS PARA REPORTES
    // ========================================

    /**
     * Genera un resumen financiero del caso
     * @return resumen detallado
     */
    public String getFinancialSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== RESUMEN FINANCIERO ===\n");
        summary.append("Caso: ").append(getBillingReference()).append("\n");
        summary.append("Cliente: ").append(getClientName()).append("\n");
        summary.append("Tipo de Facturación: ").append(billingType.getDisplayName()).append("\n");

        if (hourlyRate != null) {
            summary.append("Tarifa por Hora: $").append(hourlyRate).append("\n");
        }
        if (fixedFee != null) {
            summary.append("Tarifa Fija: $").append(fixedFee).append("\n");
        }

        summary.append("Horas Trabajadas: ").append(actualHours != null ? actualHours : "0").append("\n");
        summary.append("Ingresos Totales: $").append(totalRevenue != null ? totalRevenue : "0").append("\n");
        summary.append("Gastos Totales: $").append(totalExpenses != null ? totalExpenses : "0").append("\n");
        summary.append("Ganancia Neta: $").append(getNetProfit()).append("\n");
        summary.append("Margen de Ganancia: ").append(calculateProfitMargin()).append("%\n");
        summary.append("Estado: ").append(getFinancialStatus()).append("\n");

        if (budgetLimit != null) {
            summary.append("Presupuesto Utilizado: ").append(getBudgetUtilizationPercentage()).append("%\n");
        }

        return summary.toString();
    }

    /**
     * Obtiene métricas clave para dashboards
     * @return métricas en formato Map-friendly
     */
    public String getKeyMetrics() {
        return String.format("Revenue: $%s | Expenses: $%s | Profit: $%s | Margin: %s%% | Hours: %s",
                totalRevenue != null ? totalRevenue : "0",
                totalExpenses != null ? totalExpenses : "0",
                getNetProfit(),
                calculateProfitMargin(),
                actualHours != null ? actualHours : "0");
    }

    // ========================================
    // MÉTODOS DE CONFIGURACIÓN
    // ========================================

    /**
     * Configura facturación por horas
     * @param rate tarifa por hora
     */
    public void setupHourlyBilling(BigDecimal rate) {
        this.billingType = BillingType.HOURLY;
        this.hourlyRate = rate;
        this.fixedFee = null;
        this.contingencyPercentage = null;
    }

    /**
     * Configura facturación de tarifa fija
     * @param fee tarifa fija total
     */
    public void setupFixedFeeBilling(BigDecimal fee) {
        this.billingType = BillingType.FIXED;
        this.fixedFee = fee;
        this.hourlyRate = null;
        this.contingencyPercentage = null;
    }

    /**
     * Configura facturación de contingencia
     * @param percentage porcentaje de contingencia
     */
    public void setupContingencyBilling(BigDecimal percentage) {
        this.billingType = BillingType.CONTINGENCY;
        this.contingencyPercentage = percentage;
        this.hourlyRate = null;
        this.fixedFee = null;
    }

    // ========================================
    // MÉTODO toString PERSONALIZADO
    // ========================================

    @Override
    public String toString() {
        return String.format("FinancialCase{id=%d, case='%s', type=%s, revenue=$%s, profit=$%s}",
                id,
                legalCase != null ? legalCase.getCaseNumber() : "N/A",
                billingType,
                totalRevenue != null ? totalRevenue : "0",
                getNetProfit());
    }
}