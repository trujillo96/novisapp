// =================================================================
// FinancialCaseRepository.java
// =================================================================
// Ubicación: src/main/java/com/example/novisapp/repository/FinancialCaseRepository.java

package com.example.novisapp.repository;

import com.example.novisapp.entity.FinancialCase;
import com.example.novisapp.entity.BillingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de casos financieros
 * Incluye queries especializados para analytics, reportes y facturación
 */
@Repository
public interface FinancialCaseRepository extends JpaRepository<FinancialCase, Long> {

    // ========================================
    // BÚSQUEDAS BÁSICAS
    // ========================================

    /**
     * Buscar configuración financiera por caso legal
     */
    Optional<FinancialCase> findByLegalCaseId(Long legalCaseId);

    /**
     * Verificar si existe configuración financiera para un caso
     */
    boolean existsByLegalCaseId(Long legalCaseId);

    /**
     * Buscar casos por tipo de facturación
     */
    List<FinancialCase> findByBillingType(BillingType billingType);

    /**
     * Buscar casos por rango de tarifa por hora
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.hourlyRate BETWEEN :minRate AND :maxRate")
    List<FinancialCase> findByHourlyRateRange(@Param("minRate") BigDecimal minRate,
                                              @Param("maxRate") BigDecimal maxRate);

    // ========================================
    // BÚSQUEDAS POR PRESUPUESTO Y LÍMITES
    // ========================================

    /**
     * Buscar casos por rango de presupuesto
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.budgetLimit BETWEEN :minBudget AND :maxBudget")
    List<FinancialCase> findByBudgetRange(@Param("minBudget") BigDecimal minBudget,
                                          @Param("maxBudget") BigDecimal maxBudget);

    /**
     * Casos que exceden el presupuesto
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.totalRevenue > fc.budgetLimit AND fc.budgetLimit IS NOT NULL")
    List<FinancialCase> findCasesOverBudget();

    /**
     * Casos que exceden las horas estimadas
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.actualHours > fc.estimatedHours AND fc.estimatedHours IS NOT NULL")
    List<FinancialCase> findCasesOverEstimatedHours();

    /**
     * Casos que exceden el límite de gastos
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.totalExpenses > fc.expenseLimit AND fc.expenseLimit IS NOT NULL")
    List<FinancialCase> findCasesOverExpenseLimit();

    /**
     * Casos con alertas (cualquier tipo de exceso)
     */
    @Query("""
        SELECT fc FROM FinancialCase fc WHERE 
        (fc.totalRevenue > fc.budgetLimit AND fc.budgetLimit IS NOT NULL) OR
        (fc.actualHours > fc.estimatedHours AND fc.estimatedHours IS NOT NULL) OR
        (fc.totalExpenses > fc.expenseLimit AND fc.expenseLimit IS NOT NULL)
        """)
    List<FinancialCase> findCasesWithAlerts();

    // ========================================
    // ESTADÍSTICAS FINANCIERAS BÁSICAS
    // ========================================

    /**
     * Promedio de valor de casos
     */
    @Query("SELECT AVG(fc.totalRevenue) FROM FinancialCase fc WHERE fc.totalRevenue > 0")
    Optional<BigDecimal> getAverageCaseValue();

    /**
     * Total de ingresos de todos los casos
     */
    @Query("SELECT SUM(fc.totalRevenue) FROM FinancialCase fc")
    Optional<BigDecimal> getTotalRevenue();

    /**
     * Total de gastos de todos los casos
     */
    @Query("SELECT SUM(fc.totalExpenses) FROM FinancialCase fc")
    Optional<BigDecimal> getTotalExpenses();

    /**
     * Total de horas trabajadas en todos los casos
     */
    @Query("SELECT SUM(fc.actualHours) FROM FinancialCase fc")
    Optional<BigDecimal> getTotalHours();

    /**
     * Promedio de tarifa por hora
     */
    @Query("SELECT AVG(fc.hourlyRate) FROM FinancialCase fc WHERE fc.hourlyRate IS NOT NULL")
    Optional<BigDecimal> getAverageHourlyRate();

    /**
     * Margen de ganancia promedio
     */
    @Query("SELECT AVG(fc.profitMargin) FROM FinancialCase fc WHERE fc.profitMargin IS NOT NULL")
    Optional<BigDecimal> getAverageProfitMargin();

    // ========================================
    // CONTEOS Y ESTADÍSTICAS
    // ========================================

    /**
     * Contar casos activos (con casos legales en estado activo)
     */
    @Query("SELECT COUNT(fc) FROM FinancialCase fc JOIN fc.legalCase lc WHERE lc.status IN ('OPEN', 'IN_PROGRESS')")
    Long countActiveCases();

    /**
     * Contar casos por tipo de facturación
     */
    @Query("SELECT fc.billingType, COUNT(fc) FROM FinancialCase fc GROUP BY fc.billingType")
    List<Object[]> countCasesByBillingType();

    /**
     * Contar casos rentables
     */
    @Query("SELECT COUNT(fc) FROM FinancialCase fc WHERE fc.totalRevenue > fc.totalExpenses")
    Long countProfitableCases();

    /**
     * Contar casos por cliente
     */
    @Query("SELECT COUNT(fc) FROM FinancialCase fc WHERE fc.legalCase.client.id = :clientId")
    Long countActiveCasesByClient(@Param("clientId") Long clientId);

    // ========================================
    // BÚSQUEDAS POR FECHAS
    // ========================================

    /**
     * Casos creados en un rango de fechas
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.createdAt BETWEEN :startDate AND :endDate")
    List<FinancialCase> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Casos activos en un período específico (para reportes mensuales)
     */
    @Query("SELECT COUNT(fc) FROM FinancialCase fc WHERE fc.createdAt BETWEEN :startDate AND :endDate")
    Long countActiveCasesByMonth(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Casos con próxima fecha de facturación
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.nextBillingDate <= :date AND fc.autoInvoice = true")
    List<FinancialCase> findCasesReadyForBilling(@Param("date") LocalDateTime date);

    /**
     * Casos que necesitan facturación (sin facturar en X días)
     */
    @Query("""
        SELECT fc FROM FinancialCase fc WHERE 
        fc.lastBillingDate IS NULL OR 
        fc.lastBillingDate < :cutoffDate
        """)
    List<FinancialCase> findCasesNeedingBilling(@Param("cutoffDate") LocalDateTime cutoffDate);

    // ========================================
    // ANÁLISIS DE RENTABILIDAD
    // ========================================

    /**
     * Casos más rentables (ordenados por margen de ganancia)
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.profitMargin > :minMargin ORDER BY fc.profitMargin DESC")
    List<FinancialCase> findMostProfitableCases(@Param("minMargin") BigDecimal minMargin);

    /**
     * Casos menos rentables
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.profitMargin < :maxMargin ORDER BY fc.profitMargin ASC")
    List<FinancialCase> findLeastProfitableCases(@Param("maxMargin") BigDecimal maxMargin);

    /**
     * Casos con pérdidas
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.totalRevenue < fc.totalExpenses")
    List<FinancialCase> findLosingCases();

    /**
     * Top casos por ingresos
     */
    @Query("SELECT fc FROM FinancialCase fc ORDER BY fc.totalRevenue DESC")
    List<FinancialCase> findTopRevenueGeneratingCases(Pageable pageable);

    // ========================================
    // BÚSQUEDAS POR CLIENTE Y ABOGADO
    // ========================================

    /**
     * Casos por abogado principal
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.legalCase.primaryLawyer.id = :lawyerId")
    List<FinancialCase> findByPrimaryLawyer(@Param("lawyerId") Long lawyerId);

    /**
     * Casos por cliente específico
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.legalCase.client.id = :clientId")
    List<FinancialCase> findByClient(@Param("clientId") Long clientId);

    /**
     * Mejores clientes por ingresos
     */
    @Query("""
        SELECT fc.legalCase.client.id, fc.legalCase.client.name, SUM(fc.totalRevenue) as totalRevenue
        FROM FinancialCase fc 
        GROUP BY fc.legalCase.client.id, fc.legalCase.client.name
        ORDER BY totalRevenue DESC
        """)
    List<Object[]> findTopRevenueClients(Pageable pageable);

    /**
     * Ingresos totales por cliente
     */
    @Query("SELECT SUM(fc.totalRevenue) FROM FinancialCase fc WHERE fc.legalCase.client.id = :clientId")
    Optional<BigDecimal> getTotalRevenueByClient(@Param("clientId") Long clientId);

    // ========================================
    // ANÁLISIS DE PRODUCTIVIDAD
    // ========================================

    /**
     * Casos con mayor eficiencia (ingresos/horas)
     */
    @Query("""
        SELECT fc FROM FinancialCase fc 
        WHERE fc.actualHours > 0 
        ORDER BY (fc.totalRevenue / fc.actualHours) DESC
        """)
    List<FinancialCase> findMostEfficientCases(Pageable pageable);

    /**
     * Promedio de horas por caso
     */
    @Query("SELECT AVG(fc.actualHours) FROM FinancialCase fc WHERE fc.actualHours > 0")
    Optional<BigDecimal> getAverageHoursPerCase();

    /**
     * Casos que superaron estimaciones de tiempo
     */
    @Query("""
        SELECT fc FROM FinancialCase fc 
        WHERE fc.estimatedHours IS NOT NULL 
        AND fc.actualHours > fc.estimatedHours
        ORDER BY (fc.actualHours - fc.estimatedHours) DESC
        """)
    List<FinancialCase> findCasesExceedingTimeEstimates();

    // ========================================
    // REPORTES ESPECIALIZADOS
    // ========================================

    /**
     * Estadísticas de ingresos por tipo de facturación
     */
    @Query("""
        SELECT fc.billingType, 
               COUNT(fc) as caseCount,
               SUM(fc.totalRevenue) as totalRevenue,
               AVG(fc.totalRevenue) as avgRevenue,
               AVG(fc.profitMargin) as avgProfitMargin
        FROM FinancialCase fc 
        GROUP BY fc.billingType
        ORDER BY totalRevenue DESC
        """)
    List<Object[]> getRevenueStatsByBillingType();

    /**
     * Tendencias mensuales de ingresos
     */
    @Query("""
        SELECT EXTRACT(YEAR FROM fc.createdAt) as year,
               EXTRACT(MONTH FROM fc.createdAt) as month,
               COUNT(fc) as caseCount,
               SUM(fc.totalRevenue) as totalRevenue,
               AVG(fc.totalRevenue) as avgRevenue
        FROM FinancialCase fc 
        WHERE fc.createdAt >= :startDate
        GROUP BY EXTRACT(YEAR FROM fc.createdAt), EXTRACT(MONTH FROM fc.createdAt)
        ORDER BY year DESC, month DESC
        """)
    List<Object[]> getMonthlyRevenueTrends(@Param("startDate") LocalDateTime startDate);

    /**
     * Casos por rango de ingresos
     */
    @Query("""
        SELECT 
            CASE 
                WHEN fc.totalRevenue < 5000 THEN 'Under $5K'
                WHEN fc.totalRevenue < 25000 THEN '$5K - $25K'
                WHEN fc.totalRevenue < 100000 THEN '$25K - $100K'
                ELSE 'Over $100K'
            END as revenueRange,
            COUNT(fc) as caseCount,
            SUM(fc.totalRevenue) as totalRevenue
        FROM FinancialCase fc 
        WHERE fc.totalRevenue > 0
        GROUP BY 
            CASE 
                WHEN fc.totalRevenue < 5000 THEN 'Under $5K'
                WHEN fc.totalRevenue < 25000 THEN '$5K - $25K'
                WHEN fc.totalRevenue < 100000 THEN '$25K - $100K'
                ELSE 'Over $100K'
            END
        ORDER BY MIN(fc.totalRevenue)
        """)
    List<Object[]> getCasesByRevenueRange();

    // ========================================
    // CONFIGURACIONES ESPECIALES
    // ========================================

    /**
     * Casos con facturación automática habilitada
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.autoInvoice = true")
    List<FinancialCase> findCasesWithAutoInvoice();

    /**
     * Casos prepagados (con retainer)
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.prepaidCase = true AND fc.retainerAmount > 0")
    List<FinancialCase> findPrepaidCases();

    /**
     * Casos con retainer agotado
     */
    @Query("SELECT fc FROM FinancialCase fc WHERE fc.prepaidCase = true AND fc.totalRevenue >= fc.retainerAmount")
    List<FinancialCase> findDepletedRetainerCases();

    // ========================================
    // VALIDACIONES Y ALERTAS
    // ========================================

    /**
     * Casos con configuración incompleta
     */
    @Query("""
        SELECT fc FROM FinancialCase fc WHERE 
        (fc.billingType = 'HOURLY' AND fc.hourlyRate IS NULL) OR
        (fc.billingType = 'FIXED' AND fc.fixedFee IS NULL) OR
        (fc.billingType = 'CONTINGENCY' AND fc.contingencyPercentage IS NULL) OR
        (fc.billingType = 'RETAINER' AND fc.retainerAmount IS NULL)
        """)
    List<FinancialCase> findCasesWithIncompleteConfiguration();

    /**
     * Casos que requieren atención urgente
     */
    @Query("""
        SELECT fc FROM FinancialCase fc WHERE 
        fc.totalRevenue > COALESCE(fc.budgetLimit, 999999) OR
        fc.actualHours > COALESCE(fc.estimatedHours, 9999) OR
        fc.totalExpenses > COALESCE(fc.expenseLimit, 999999) OR
        (fc.prepaidCase = true AND fc.totalRevenue >= fc.retainerAmount)
        """)
    List<FinancialCase> findCasesRequiringAttention();

    // ========================================
    // BÚSQUEDAS AVANZADAS CON FILTROS
    // ========================================

    /**
     * Búsqueda avanzada con múltiples criterios
     */
    @Query("""
        SELECT fc FROM FinancialCase fc WHERE
        (:billingType IS NULL OR fc.billingType = :billingType) AND
        (:minRevenue IS NULL OR fc.totalRevenue >= :minRevenue) AND
        (:maxRevenue IS NULL OR fc.totalRevenue <= :maxRevenue) AND
        (:clientId IS NULL OR fc.legalCase.client.id = :clientId) AND
        (:lawyerId IS NULL OR fc.legalCase.primaryLawyer.id = :lawyerId)
        ORDER BY fc.totalRevenue DESC
        """)
    List<FinancialCase> findByCriteria(
            @Param("billingType") BillingType billingType,
            @Param("minRevenue") BigDecimal minRevenue,
            @Param("maxRevenue") BigDecimal maxRevenue,
            @Param("clientId") Long clientId,
            @Param("lawyerId") Long lawyerId,
            Pageable pageable);

    /**
     * Contar casos por criterios avanzados
     */
    @Query("""
        SELECT COUNT(fc) FROM FinancialCase fc WHERE
        (:billingType IS NULL OR fc.billingType = :billingType) AND
        (:minRevenue IS NULL OR fc.totalRevenue >= :minRevenue) AND
        (:maxRevenue IS NULL OR fc.totalRevenue <= :maxRevenue) AND
        (:clientId IS NULL OR fc.legalCase.client.id = :clientId) AND
        (:lawyerId IS NULL OR fc.legalCase.primaryLawyer.id = :lawyerId)
        """)
    Long countByCriteria(
            @Param("billingType") BillingType billingType,
            @Param("minRevenue") BigDecimal minRevenue,
            @Param("maxRevenue") BigDecimal maxRevenue,
            @Param("clientId") Long clientId,
            @Param("lawyerId") Long lawyerId);
}