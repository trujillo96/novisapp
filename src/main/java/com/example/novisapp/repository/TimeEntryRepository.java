// =================================================================
// TimeEntryRepository.java - VERSIÓN FINAL CORREGIDA
// =================================================================
// Ubicación: src/main/java/com/example/novisapp/repository/TimeEntryRepository.java

package com.example.novisapp.repository;

import com.example.novisapp.entity.TimeEntry;
import com.example.novisapp.entity.TimeEntryStatus;
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
 * Repositorio para operaciones de entradas de tiempo
 * Incluye queries para tracking, facturación, analytics y reportes
 */
@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    // ========================================
    // BÚSQUEDAS BÁSICAS POR CASO
    // ========================================

    /**
     * Entradas de tiempo por caso (ordenadas por fecha de creación)
     */
    List<TimeEntry> findByLegalCaseIdOrderByCreatedAtDesc(Long legalCaseId);

    /**
     * Todas las entradas de tiempo por caso
     */
    List<TimeEntry> findByLegalCaseId(Long legalCaseId);

    /**
     * Entradas de tiempo facturables por caso
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.legalCase.id = :caseId AND te.billable = true")
    List<TimeEntry> findBillableEntriesByCase(@Param("caseId") Long caseId);

    /**
     * Entradas de tiempo no facturadas por caso
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.legalCase.id = :caseId AND te.status = 'APPROVED' AND te.billed = false")
    List<TimeEntry> findUnbilledEntriesByCase(@Param("caseId") Long caseId);

    // ========================================
    // BÚSQUEDAS POR ABOGADO
    // ========================================

    /**
     * Entradas de tiempo por abogado (ordenadas por fecha de trabajo)
     */
    List<TimeEntry> findByLawyerIdOrderByWorkDateDesc(Long lawyerId);

    /**
     * Entradas de tiempo por abogado en un rango de fechas
     */
    List<TimeEntry> findByLawyerIdAndWorkDateBetweenOrderByWorkDateDesc(
            Long lawyerId, LocalDate startDate, LocalDate endDate);

    /**
     * Entradas de tiempo por caso y abogado
     */
    List<TimeEntry> findByLegalCaseIdAndLawyerId(Long legalCaseId, Long lawyerId);

    /**
     * Entradas de tiempo del día actual por abogado
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.lawyer.id = :lawyerId AND te.workDate = CURRENT_DATE")
    List<TimeEntry> findTodayEntriesByLawyer(@Param("lawyerId") Long lawyerId);

    // ========================================
    // BÚSQUEDAS POR ESTADO
    // ========================================

    /**
     * Entradas de tiempo por estado
     */
    List<TimeEntry> findByStatus(TimeEntryStatus status);

    /**
     * Entradas de tiempo por estado y abogado
     */
    List<TimeEntry> findByStatusAndLawyerId(TimeEntryStatus status, Long lawyerId);

    /**
     * Entradas pendientes de aprobación (ordenadas por fecha de creación)
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.status = 'SUBMITTED' ORDER BY te.createdAt ASC")
    List<TimeEntry> findPendingApproval();

    /**
     * Entradas listas para facturar
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.status = 'APPROVED' AND te.billable = true AND te.billed = false")
    List<TimeEntry> findReadyToBill();

    /**
     * Entradas rechazadas por abogado
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.status = 'REJECTED' AND te.lawyer.id = :lawyerId ORDER BY te.updatedAt DESC")
    List<TimeEntry> findRejectedEntriesByLawyer(@Param("lawyerId") Long lawyerId);

    // ========================================
    // BÚSQUEDAS POR FECHA
    // ========================================

    /**
     * Entradas de tiempo por rango de fechas de trabajo
     */
    List<TimeEntry> findByWorkDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Entradas de tiempo de hoy
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.workDate = CURRENT_DATE")
    List<TimeEntry> findTodayEntries();

    /**
     * Entradas de tiempo de la semana actual
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.workDate >= :weekStart AND te.workDate <= :weekEnd")
    List<TimeEntry> findWeekEntries(@Param("weekStart") LocalDate weekStart, @Param("weekEnd") LocalDate weekEnd);

    /**
     * Entradas de trabajo de fin de semana - SIMPLIFICADO
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.workDate >= :startDate")
    List<TimeEntry> findWeekendEntries(@Param("startDate") LocalDate startDate);

    // ========================================
    // ESTADÍSTICAS DE TIEMPO Y REVENUE
    // ========================================

    /**
     * Total de horas facturables por caso
     */
    @Query("SELECT SUM(te.duration) FROM TimeEntry te WHERE te.legalCase.id = :caseId AND te.billable = true")
    Optional<BigDecimal> getTotalBillableHoursByCase(@Param("caseId") Long caseId);

    /**
     * Total de ingresos por caso
     */
    @Query("SELECT SUM(te.totalAmount) FROM TimeEntry te WHERE te.legalCase.id = :caseId AND te.billable = true")
    Optional<BigDecimal> getTotalRevenueByCase(@Param("caseId") Long caseId);

    /**
     * Total de ingresos por rango de fechas
     */
    @Query("SELECT SUM(te.totalAmount) FROM TimeEntry te WHERE te.workDate BETWEEN :startDate AND :endDate AND te.billable = true")
    Optional<BigDecimal> getTotalRevenueByDateRange(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    /**
     * Total de horas por rango de fechas
     */
    @Query("SELECT SUM(te.duration) FROM TimeEntry te WHERE te.workDate BETWEEN :startDate AND :endDate")
    Optional<BigDecimal> getTotalHoursByDateRange(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    /**
     * Promedio de tarifa por hora
     */
    @Query("SELECT AVG(te.hourlyRate) FROM TimeEntry te WHERE te.billable = true")
    Optional<BigDecimal> getAverageHourlyRate();

    /**
     * Total de ingresos no facturados
     */
    @Query("SELECT SUM(te.totalAmount) FROM TimeEntry te WHERE te.status = 'APPROVED' AND te.billable = true AND te.billed = false")
    Optional<BigDecimal> getTotalUnbilledRevenue();

    // ========================================
    // ESTADÍSTICAS POR ABOGADO
    // ========================================

    /**
     * Total de horas por abogado en un período
     */
    @Query("SELECT SUM(te.duration) FROM TimeEntry te WHERE te.lawyer.id = :lawyerId AND te.workDate BETWEEN :startDate AND :endDate")
    Optional<BigDecimal> getTotalHoursByLawyer(@Param("lawyerId") Long lawyerId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /**
     * Total de ingresos por abogado
     */
    @Query("SELECT SUM(te.totalAmount) FROM TimeEntry te WHERE te.lawyer.id = :lawyerId AND te.billable = true")
    Optional<BigDecimal> getTotalRevenueByLawyer(@Param("lawyerId") Long lawyerId);

    /**
     * Promedio de horas diarias por abogado - SIMPLIFICADO Y CORREGIDO
     */
    @Query("SELECT AVG(te.duration) FROM TimeEntry te WHERE te.lawyer.id = :lawyerId AND te.workDate BETWEEN :startDate AND :endDate AND te.duration > 0")
    Optional<BigDecimal> getAverageDailyHoursByLawyer(@Param("lawyerId") Long lawyerId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * Día con más horas trabajadas por abogado
     */
    @Query("SELECT te.workDate, SUM(te.duration) FROM TimeEntry te WHERE te.lawyer.id = :lawyerId GROUP BY te.workDate ORDER BY SUM(te.duration) DESC")
    List<Object[]> findPeakWorkDaysByLawyer(@Param("lawyerId") Long lawyerId, Pageable pageable);

    // ========================================
    // ESTADÍSTICAS POR CLIENTE
    // ========================================

    /**
     * Total de ingresos por cliente
     */
    @Query("SELECT SUM(te.totalAmount) FROM TimeEntry te WHERE te.legalCase.client.id = :clientId AND te.billable = true")
    Optional<BigDecimal> getTotalRevenueByClient(@Param("clientId") Long clientId);

    /**
     * Total de horas por cliente
     */
    @Query("SELECT SUM(te.duration) FROM TimeEntry te WHERE te.legalCase.client.id = :clientId")
    Optional<BigDecimal> getTotalHoursByClient(@Param("clientId") Long clientId);

    // ========================================
    // TOP PERFORMERS Y RANKINGS
    // ========================================

    /**
     * Top abogados por ingresos en un período
     */
    @Query("SELECT te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName, SUM(te.totalAmount) FROM TimeEntry te WHERE te.billable = true AND te.workDate BETWEEN :startDate AND :endDate GROUP BY te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName ORDER BY SUM(te.totalAmount) DESC")
    Page<Object[]> findTopRevenueGeneratingLawyers(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   Pageable pageable);

    /**
     * Top abogados por horas trabajadas
     */
    @Query("SELECT te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName, SUM(te.duration) FROM TimeEntry te WHERE te.workDate BETWEEN :startDate AND :endDate GROUP BY te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName ORDER BY SUM(te.duration) DESC")
    List<Object[]> findTopHourWorkers(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      Pageable pageable);

    /**
     * Abogados más eficientes (mayor revenue por hora) - SIMPLIFICADO
     */
    @Query("SELECT te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName, SUM(te.totalAmount), SUM(te.duration) FROM TimeEntry te WHERE te.billable = true AND te.workDate BETWEEN :startDate AND :endDate GROUP BY te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName HAVING SUM(te.duration) > 0 ORDER BY (SUM(te.totalAmount) / SUM(te.duration)) DESC")
    List<Object[]> findMostEfficientLawyers(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            Pageable pageable);

    // ========================================
    // ANÁLISIS DE PRODUCTIVIDAD - SIMPLIFICADO
    // ========================================

    /**
     * Productividad por día de la semana - SIMPLIFICADO
     */
    @Query("SELECT te.workDate, AVG(te.duration), COUNT(te) FROM TimeEntry te WHERE te.workDate BETWEEN :startDate AND :endDate GROUP BY te.workDate ORDER BY te.workDate")
    List<Object[]> getProductivityByDayOfWeek(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /**
     * Horas trabajadas por categoría de tarea
     */
    @Query("SELECT COALESCE(te.taskCategory, 'Sin Categoría'), SUM(te.duration), COUNT(te), AVG(te.duration) FROM TimeEntry te WHERE te.workDate BETWEEN :startDate AND :endDate GROUP BY te.taskCategory ORDER BY SUM(te.duration) DESC")
    List<Object[]> getHoursByTaskCategory(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * Análisis de overtime (entradas de más de 8 horas)
     */
    @Query("SELECT te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName, SUM(CASE WHEN te.duration > 8 THEN te.duration ELSE 0 END), SUM(te.duration) FROM TimeEntry te WHERE te.workDate BETWEEN :startDate AND :endDate GROUP BY te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName HAVING SUM(CASE WHEN te.duration > 8 THEN te.duration ELSE 0 END) > 0 ORDER BY SUM(CASE WHEN te.duration > 8 THEN te.duration ELSE 0 END) DESC")
    List<Object[]> getOvertimeAnalysis(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    /**
     * Sesiones de trabajo largas (más de 6 horas)
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.duration > 6 ORDER BY te.duration DESC")
    List<TimeEntry> findLongWorkSessions(Pageable pageable);

    // ========================================
    // ANÁLISIS TEMPORAL - SIMPLIFICADO
    // ========================================

    /**
     * Tendencias mensuales de horas - SIMPLIFICADO
     */
    @Query("SELECT te.workDate, SUM(te.duration), COUNT(te), SUM(te.totalAmount) FROM TimeEntry te WHERE te.workDate >= :startDate GROUP BY te.workDate ORDER BY te.workDate DESC")
    List<Object[]> getMonthlyTrends(@Param("startDate") LocalDate startDate);

    /**
     * Comparación de productividad mes a mes - SIMPLIFICADO
     */
    @Query("SELECT te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName, te.workDate, SUM(te.duration), SUM(te.totalAmount) FROM TimeEntry te WHERE te.workDate BETWEEN :startDate AND :endDate GROUP BY te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName, te.workDate ORDER BY te.lawyer.id, te.workDate")
    List<Object[]> getMonthlyProductivityByLawyer(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    // ========================================
    // ALERTAS Y VALIDACIONES
    // ========================================

    /**
     * Entradas con alta duración que pueden necesitar revisión
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.duration > :threshold AND te.status = 'SUBMITTED'")
    List<TimeEntry> findHighDurationEntries(@Param("threshold") BigDecimal threshold);

    /**
     * Entradas con tarifas inusuales (muy altas o muy bajas)
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.hourlyRate < :minRate OR te.hourlyRate > :maxRate ORDER BY te.hourlyRate DESC")
    List<TimeEntry> findUnusualRateEntries(@Param("minRate") BigDecimal minRate,
                                           @Param("maxRate") BigDecimal maxRate);

    /**
     * Entradas antiguas sin procesar (más de X días)
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.status = 'DRAFT' AND te.createdAt < :cutoffDate")
    List<TimeEntry> findOldUnprocessedEntries(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Entradas duplicadas posibles (mismo abogado, caso, fecha, duración similar)
     */
    @Query("SELECT te FROM TimeEntry te WHERE EXISTS (SELECT 1 FROM TimeEntry te2 WHERE te2.id != te.id AND te2.lawyer.id = te.lawyer.id AND te2.legalCase.id = te.legalCase.id AND te2.workDate = te.workDate AND ABS(te2.duration - te.duration) < 0.1) ORDER BY te.lawyer.id, te.workDate")
    List<TimeEntry> findPotentialDuplicates();

    // ========================================
    // BÚSQUEDAS PARA FACTURACIÓN
    // ========================================

    /**
     * Entradas aprobadas listas para incluir en factura
     */
    @Query("SELECT te FROM TimeEntry te WHERE te.legalCase.id = :caseId AND te.status = 'APPROVED' AND te.billable = true AND te.billed = false AND te.workDate BETWEEN :startDate AND :endDate ORDER BY te.workDate, te.createdAt")
    List<TimeEntry> findEntriesForBilling(@Param("caseId") Long caseId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * Resumen de facturación por caso
     */
    @Query("SELECT te.legalCase.id, te.legalCase.caseNumber, COUNT(te), SUM(te.duration), SUM(te.totalAmount) FROM TimeEntry te WHERE te.status = 'APPROVED' AND te.billable = true AND te.billed = false GROUP BY te.legalCase.id, te.legalCase.caseNumber HAVING SUM(te.totalAmount) > 0 ORDER BY SUM(te.totalAmount) DESC")
    List<Object[]> getBillingSummaryByCases();

    // ========================================
    // BÚSQUEDAS PARA REPORTES EJECUTIVOS
    // ========================================

    /**
     * KPIs principales del período
     */
    @Query("SELECT COUNT(te), SUM(te.duration), SUM(CASE WHEN te.billable = true THEN te.duration ELSE 0 END), SUM(CASE WHEN te.billable = true THEN te.totalAmount ELSE 0 END), AVG(te.hourlyRate), COUNT(DISTINCT te.lawyer.id), COUNT(DISTINCT te.legalCase.id) FROM TimeEntry te WHERE te.workDate BETWEEN :startDate AND :endDate")
    Object[] getKPIsForPeriod(@Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    /**
     * Utilización por abogado (porcentaje de horas facturables)
     */
    @Query("SELECT te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName, SUM(te.duration), SUM(CASE WHEN te.billable = true THEN te.duration ELSE 0 END) FROM TimeEntry te WHERE te.workDate BETWEEN :startDate AND :endDate GROUP BY te.lawyer.id, te.lawyer.firstName, te.lawyer.lastName HAVING SUM(te.duration) > 0 ORDER BY (SUM(CASE WHEN te.billable = true THEN te.duration ELSE 0 END) / SUM(te.duration) * 100) DESC")
    List<Object[]> getUtilizationByLawyer(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    // ========================================
    // BÚSQUEDAS PERSONALIZADAS
    // ========================================

    /**
     * Búsqueda avanzada con múltiples filtros
     */
    @Query("SELECT te FROM TimeEntry te WHERE (:lawyerId IS NULL OR te.lawyer.id = :lawyerId) AND (:caseId IS NULL OR te.legalCase.id = :caseId) AND (:status IS NULL OR te.status = :status) AND (:billable IS NULL OR te.billable = :billable) AND (:startDate IS NULL OR te.workDate >= :startDate) AND (:endDate IS NULL OR te.workDate <= :endDate) AND (:minDuration IS NULL OR te.duration >= :minDuration) AND (:maxDuration IS NULL OR te.duration <= :maxDuration) ORDER BY te.workDate DESC, te.createdAt DESC")
    List<TimeEntry> findByCriteria(
            @Param("lawyerId") Long lawyerId,
            @Param("caseId") Long caseId,
            @Param("status") TimeEntryStatus status,
            @Param("billable") Boolean billable,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("minDuration") BigDecimal minDuration,
            @Param("maxDuration") BigDecimal maxDuration,
            Pageable pageable);

    /**
     * Contar entradas por criterios
     */
    @Query("SELECT COUNT(te) FROM TimeEntry te WHERE (:lawyerId IS NULL OR te.lawyer.id = :lawyerId) AND (:caseId IS NULL OR te.legalCase.id = :caseId) AND (:status IS NULL OR te.status = :status) AND (:billable IS NULL OR te.billable = :billable) AND (:startDate IS NULL OR te.workDate >= :startDate) AND (:endDate IS NULL OR te.workDate <= :endDate)")
    Long countByCriteria(
            @Param("lawyerId") Long lawyerId,
            @Param("caseId") Long caseId,
            @Param("status") TimeEntryStatus status,
            @Param("billable") Boolean billable,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}