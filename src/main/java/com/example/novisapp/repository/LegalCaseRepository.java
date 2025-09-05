package com.example.novisapp.repository;

// ✅ IMPORTS CORREGIDOS - Todos los enums deben estar en entity
import com.example.novisapp.entity.CaseStatus;
import com.example.novisapp.entity.Country;
import com.example.novisapp.entity.LegalSpecialty;
import com.example.novisapp.entity.LegalCase;
import com.example.novisapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LegalCaseRepository extends JpaRepository<LegalCase, Long> {

    // ========================================
    // BÚSQUEDAS BÁSICAS EXISTENTES
    // ========================================

    /**
     * Buscar casos por número de caso
     */
    Optional<LegalCase> findByCaseNumber(String caseNumber);

    /**
     * Buscar casos por estado
     */
    List<LegalCase> findByStatus(CaseStatus status);

    /**
     * Buscar casos por múltiples estados
     */
    List<LegalCase> findByStatusIn(List<CaseStatus> statuses);

    /**
     * Contar casos por estado
     */
    Long countByStatus(CaseStatus status);

    /**
     * Contar casos por múltiples estados
     */
    Long countByStatusIn(List<CaseStatus> statuses);

    /**
     * Buscar casos por cliente
     */
    List<LegalCase> findByClientId(Long clientId);

    /**
     * Buscar casos por abogado principal
     */
    List<LegalCase> findByPrimaryLawyer(User primaryLawyer);

    /**
     * Buscar casos por tipo
     */
    List<LegalCase> findByCaseType(String caseType);

    /**
     * Buscar casos por prioridad
     */
    List<LegalCase> findByPriority(String priority);

    /**
     * Contar casos por prioridad
     */
    @Query("SELECT COUNT(c) FROM LegalCase c WHERE c.priority = :priority")
    Long countByPriority(@Param("priority") String priority);

    /**
     * Contar casos que coinciden con un patrón de número
     */
    @Query("SELECT COUNT(c) FROM LegalCase c WHERE c.caseNumber LIKE :pattern")
    Long countByCaseNumberLike(@Param("pattern") String pattern);

    // ========================================
    // BÚSQUEDAS POR FECHA
    // ========================================

    /**
     * Buscar casos creados entre fechas
     */
    List<LegalCase> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Buscar casos con fecha de completación esperada
     */
    List<LegalCase> findByExpectedCompletionDateBefore(LocalDateTime date);

    /**
     * Buscar casos vencidos
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.expectedCompletionDate < CURRENT_TIMESTAMP " +
            "AND lc.status NOT IN ('COMPLETED', 'CLOSED')")
    List<LegalCase> findOverdueCases();

    /**
     * Buscar casos vencidos con parámetro
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.expectedCompletionDate < :currentDate " +
            "AND lc.status NOT IN ('COMPLETED', 'CLOSED')")
    List<LegalCase> findOverdueCases(@Param("currentDate") LocalDateTime currentDate);

    // ========================================
    // MÉTODOS PARA TEAM DASHBOARD (CORREGIDOS)
    // ========================================

    /**
     * Buscar casos que NO están en un estado específico (NUEVO)
     */
    List<LegalCase> findByStatusNot(CaseStatus status);

    /**
     * Buscar abogados asignados a un caso específico (NUEVO)
     */
    @Query("SELECT u FROM User u JOIN u.assignedCases lc WHERE lc.id = :caseId")
    List<User> findAssignedLawyersByCaseId(@Param("caseId") Long caseId);

    /**
     * Método alternativo para buscar abogados asignados usando join table (NUEVO)
     */
    @Query("SELECT u FROM User u JOIN LegalCase lc ON u MEMBER OF lc.assignedLawyers WHERE lc.id = :caseId")
    List<User> findLawyersAssignedToCase(@Param("caseId") Long caseId);

    /**
     * Buscar casos sin abogados asignados
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.assignedLawyers IS EMPTY " +
            "AND lc.status IN ('OPEN', 'IN_PROGRESS')")
    List<LegalCase> findCasesWithoutAssignedLawyers();

    /**
     * Buscar casos con menos de 2 abogados (insuficientes)
     */
    @Query("SELECT lc FROM LegalCase lc WHERE SIZE(lc.assignedLawyers) < 2 " +
            "AND lc.status IN ('OPEN', 'IN_PROGRESS')")
    List<LegalCase> findCasesWithInsufficientLawyers();

    /**
     * Buscar casos activos asignados a un abogado específico
     */
    @Query("SELECT lc FROM LegalCase lc JOIN lc.assignedLawyers u " +
            "WHERE u.id = :lawyerId AND lc.status IN ('OPEN', 'IN_PROGRESS')")
    List<LegalCase> findActiveCasesAssignedToLawyer(@Param("lawyerId") Long lawyerId);

    /**
     * Buscar TODOS los casos asignados a un abogado (activos e inactivos)
     */
    @Query("SELECT lc FROM LegalCase lc JOIN lc.assignedLawyers u WHERE u.id = :lawyerId")
    List<LegalCase> findCasesAssignedToLawyer(@Param("lawyerId") Long lawyerId);

    /**
     * Contar casos asignados a un abogado
     */
    @Query("SELECT COUNT(c) FROM LegalCase c JOIN c.assignedLawyers l WHERE l.id = :lawyerId")
    Long countByAssignedLawyersId(@Param("lawyerId") Long lawyerId);

    /**
     * Buscar casos sin abogado principal asignado
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.primaryLawyer IS NULL " +
            "AND lc.status IN ('OPEN', 'IN_PROGRESS')")
    List<LegalCase> findCasesWithoutPrimaryLawyer();

    // ========================================
    // BÚSQUEDAS AVANZADAS PARA ASIGNACIÓN
    // ========================================

    /**
     * Buscar casos sin equipo asignado (para asignación automática)
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.teamAssigned = false " +
            "AND lc.status = 'OPEN'")
    List<LegalCase> findUnassignedOpenCases();

    /**
     * Buscar casos por especialidad requerida
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.requiredSpecialty = :specialty " +
            "AND lc.teamAssigned = false")
    List<LegalCase> findUnassignedCasesBySpecialty(@Param("specialty") LegalSpecialty specialty);

    /**
     * Buscar casos por país y estado
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.country = :country " +
            "AND lc.status IN :statuses")
    List<LegalCase> findByCountryAndStatusIn(@Param("country") Country country,
                                             @Param("statuses") List<CaseStatus> statuses);

    // ========================================
    // ESTADÍSTICAS Y REPORTES
    // ========================================

    /**
     * Contar casos sin equipo asignado
     */
    @Query("SELECT COUNT(lc) FROM LegalCase lc WHERE lc.teamAssigned = false " +
            "AND lc.status = 'OPEN'")
    Integer countUnassignedOpenCases();

    /**
     * Estadísticas de casos por estado
     */
    @Query("SELECT lc.status, COUNT(lc) FROM LegalCase lc GROUP BY lc.status")
    List<Object[]> getCaseStatistics();

    /**
     * Estadísticas adicionales por estado
     */
    @Query("SELECT lc.status, COUNT(lc) FROM LegalCase lc GROUP BY lc.status")
    List<Object[]> countCasesByStatus();

    /**
     * Estadísticas de casos por prioridad
     */
    @Query("SELECT lc.priority, COUNT(lc) FROM LegalCase lc " +
            "WHERE lc.status IN ('OPEN', 'IN_PROGRESS') GROUP BY lc.priority")
    List<Object[]> getCaseStatisticsByPriority();

    /**
     * Estadísticas de casos por especialidad
     */
    @Query("SELECT lc.requiredSpecialty, COUNT(lc) FROM LegalCase lc " +
            "GROUP BY lc.requiredSpecialty")
    List<Object[]> getCaseStatisticsBySpecialty();

    /**
     * Estadísticas de casos por país
     */
    @Query("SELECT lc.country, COUNT(lc) FROM LegalCase lc GROUP BY lc.country")
    List<Object[]> getCaseStatisticsByCountry();

    /**
     * Promedio de abogados por caso
     */
    @Query("SELECT AVG(SIZE(lc.assignedLawyers)) FROM LegalCase lc " +
            "WHERE lc.status IN ('OPEN', 'IN_PROGRESS')")
    Double getAverageLawyersPerCase();

    // ========================================
    // BÚSQUEDAS POR CLIENTE
    // ========================================

    /**
     * Buscar casos por país del cliente
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.client.country = :country")
    List<LegalCase> findByClientCountry(@Param("country") String country);

    /**
     * Buscar casos activos por cliente
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.client.id = :clientId " +
            "AND lc.status IN ('OPEN', 'IN_PROGRESS')")
    List<LegalCase> findActiveCasesByClient(@Param("clientId") Long clientId);

    // ========================================
    // BÚSQUEDAS POR VALOR Y DOCUMENTOS
    // ========================================

    /**
     * Buscar casos por rango de valor estimado
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.estimatedValue BETWEEN :minValue AND :maxValue")
    List<LegalCase> findByEstimatedValueBetween(@Param("minValue") java.math.BigDecimal minValue,
                                                @Param("maxValue") java.math.BigDecimal maxValue);

    /**
     * Buscar casos con muchos documentos
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.documentCount > :threshold")
    List<LegalCase> findCasesWithManyDocuments(@Param("threshold") Integer threshold);

    /**
     * Buscar casos que necesitan seguimiento post-entrega
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.postDeliveryFollowUp = false " +
            "AND lc.status = 'COMPLETED' " +
            "AND lc.actualCompletionDate < :cutoffDate")
    List<LegalCase> findCasesNeedingFollowUp(@Param("cutoffDate") LocalDateTime cutoffDate);

    // ========================================
    // MÉTODOS DE BÚSQUEDA TEXTUAL
    // ========================================

    /**
     * Buscar casos por título (contiene texto)
     */
    @Query("SELECT lc FROM LegalCase lc WHERE LOWER(lc.title) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<LegalCase> findByTitleContaining(@Param("searchText") String searchText);

    /**
     * Buscar casos por descripción
     */
    @Query("SELECT lc FROM LegalCase lc WHERE LOWER(lc.description) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<LegalCase> findByDescriptionContaining(@Param("searchText") String searchText);

    /**
     * Búsqueda general en título y descripción
     */
    @Query("SELECT lc FROM LegalCase lc WHERE " +
            "LOWER(lc.title) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
            "LOWER(lc.description) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<LegalCase> findByTitleOrDescriptionContaining(@Param("searchText") String searchText);

    // ========================================
    // MÉTODOS PARA DASHBOARD DE GESTIÓN
    // ========================================

    /**
     * Casos que requieren atención urgente
     */
    @Query("SELECT lc FROM LegalCase lc WHERE " +
            "(lc.priority IN ('HIGH', 'URGENT') AND lc.status = 'OPEN') OR " +
            "(lc.expectedCompletionDate < CURRENT_TIMESTAMP AND lc.status IN ('OPEN', 'IN_PROGRESS')) OR " +
            "(SIZE(lc.assignedLawyers) < 2 AND lc.status IN ('OPEN', 'IN_PROGRESS'))")
    List<LegalCase> findCasesRequiringAttention();

    /**
     * Casos recientes (últimos 30 días)
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.createdAt >= :thirtyDaysAgo " +
            "ORDER BY lc.createdAt DESC")
    List<LegalCase> findRecentCases(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);

    /**
     * Casos activos
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.status IN ('OPEN', 'IN_PROGRESS')")
    List<LegalCase> findActiveCases();

    /**
     * Casos completados en el último período
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.status IN ('COMPLETED', 'CLOSED') " +
            "AND lc.actualCompletionDate >= :startDate " +
            "ORDER BY lc.actualCompletionDate DESC")
    List<LegalCase> findCompletedCasesSince(@Param("startDate") LocalDateTime startDate);

    // ========================================
    // MÉTODOS ADICIONALES PARA TEAM DASHBOARD
    // ========================================

    /**
     * Contar casos activos (no cerrados) - NUEVO
     */
    @Query("SELECT COUNT(lc) FROM LegalCase lc WHERE lc.status != 'CLOSED'")
    long countActiveCases();

    /**
     * Buscar casos abiertos sin equipo asignado - NUEVO
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.status = 'OPEN' AND SIZE(lc.assignedLawyers) = 0")
    List<LegalCase> findOpenCasesWithoutTeam();

    /**
     * Buscar casos con pocos abogados asignados - NUEVO
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.status IN ('OPEN', 'IN_PROGRESS') AND SIZE(lc.assignedLawyers) < 2")
    List<LegalCase> findCasesNeedingMoreLawyers();

    /**
     * Contar casos por abogado específico - NUEVO
     */
    @Query("SELECT COUNT(lc) FROM LegalCase lc JOIN lc.assignedLawyers u WHERE u.id = :lawyerId")
    long countCasesByLawyerId(@Param("lawyerId") Long lawyerId);

    /**
     * Buscar casos activos sin contar cerrados ni completados - NUEVO
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.status NOT IN ('CLOSED', 'COMPLETED', 'CANCELLED')")
    List<LegalCase> findActiveOpenCases();

    /**
     * Buscar casos por título (búsqueda simple) - NUEVO
     */
    @Query("SELECT lc FROM LegalCase lc WHERE LOWER(lc.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<LegalCase> findByTitleContainingIgnoreCase(@Param("title") String title);

    /**
     * Método para obtener resumen de casos por estado - NUEVO
     */
    @Query("SELECT lc.status, COUNT(lc) FROM LegalCase lc GROUP BY lc.status")
    List<Object[]> getCaseCountByStatus();

    /**
     * Buscar casos urgentes que necesitan atención - NUEVO
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.priority IN ('HIGH', 'URGENT') AND lc.status IN ('OPEN', 'IN_PROGRESS')")
    List<LegalCase> findUrgentCases();

    /**
     * Buscar casos asignados a abogado principal - NUEVO
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.primaryLawyer.id = :lawyerId")
    List<LegalCase> findByPrimaryLawyerId(@Param("lawyerId") Long lawyerId);

    /**
     * Buscar casos sin abogado principal asignado - NUEVO
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.primaryLawyer IS NULL AND lc.status IN ('OPEN', 'IN_PROGRESS')")
    List<LegalCase> findCasesWithoutPrimaryLawyerAssigned();

    // ========================================
    // MÉTODOS PARA PAGINACIÓN Y FILTROS (NUEVOS)
    // ========================================

    /**
     * Buscar casos con paginación por estado
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.status = :status ORDER BY lc.createdAt DESC")
    List<LegalCase> findByStatusOrderByCreatedAtDesc(@Param("status") CaseStatus status);

    /**
     * Buscar casos por cliente con ordenamiento
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.client.id = :clientId ORDER BY lc.createdAt DESC")
    List<LegalCase> findByClientIdOrderByCreatedAtDesc(@Param("clientId") Long clientId);

    /**
     * Buscar casos por abogado principal con ordenamiento
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.primaryLawyer.id = :lawyerId ORDER BY lc.createdAt DESC")
    List<LegalCase> findByPrimaryLawyerIdOrderByCreatedAtDesc(@Param("lawyerId") Long lawyerId);

    /**
     * Buscar casos por múltiples criterios (búsqueda avanzada)
     */
    @Query("SELECT lc FROM LegalCase lc WHERE " +
            "(:status IS NULL OR lc.status = :status) AND " +
            "(:priority IS NULL OR lc.priority = :priority) AND " +
            "(:clientId IS NULL OR lc.client.id = :clientId) AND " +
            "(:searchText IS NULL OR LOWER(lc.title) LIKE LOWER(CONCAT('%', :searchText, '%')) OR LOWER(lc.description) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
            "ORDER BY lc.createdAt DESC")
    List<LegalCase> findByCriteria(@Param("status") CaseStatus status,
                                   @Param("priority") String priority,
                                   @Param("clientId") Long clientId,
                                   @Param("searchText") String searchText);

    /**
     * Contar casos por múltiples criterios
     */
    @Query("SELECT COUNT(lc) FROM LegalCase lc WHERE " +
            "(:status IS NULL OR lc.status = :status) AND " +
            "(:priority IS NULL OR lc.priority = :priority) AND " +
            "(:clientId IS NULL OR lc.client.id = :clientId) AND " +
            "(:searchText IS NULL OR LOWER(lc.title) LIKE LOWER(CONCAT('%', :searchText, '%')) OR LOWER(lc.description) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    long countByCriteria(@Param("status") CaseStatus status,
                         @Param("priority") String priority,
                         @Param("clientId") Long clientId,
                         @Param("searchText") String searchText);

    // ========================================
    // MÉTODOS PARA REPORTES AVANZADOS (NUEVOS)
    // ========================================

    /**
     * Obtener casos con mayor tiempo de resolución
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.actualCompletionDate IS NOT NULL " +
            "ORDER BY (lc.actualCompletionDate - lc.createdAt) DESC")
    List<LegalCase> findCasesWithLongestResolutionTime();

    /**
     * Obtener casos completados en tiempo récord
     */
    @Query("SELECT lc FROM LegalCase lc WHERE lc.actualCompletionDate IS NOT NULL " +
            "AND lc.expectedCompletionDate IS NOT NULL " +
            "AND lc.actualCompletionDate < lc.expectedCompletionDate " +
            "ORDER BY (lc.expectedCompletionDate - lc.actualCompletionDate) DESC")
    List<LegalCase> findCasesCompletedAheadOfSchedule();

    /**
     * Obtener estadísticas de rendimiento por período
     */
    @Query("SELECT " +
            "COUNT(lc) as total_cases, " +
            "COUNT(CASE WHEN lc.status = 'COMPLETED' THEN 1 END) as completed_cases, " +
            "COUNT(CASE WHEN lc.actualCompletionDate < lc.expectedCompletionDate THEN 1 END) as on_time_cases " +
            "FROM LegalCase lc WHERE lc.createdAt BETWEEN :startDate AND :endDate")
    List<Object[]> getPerformanceStatistics(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
}