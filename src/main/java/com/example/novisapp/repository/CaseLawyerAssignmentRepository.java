package com.example.novisapp.repository;

import com.example.novisapp.entity.CaseLawyerAssignment;
import com.example.novisapp.entity.AssignmentStatus;
import com.example.novisapp.entity.LegalCase;
import com.example.novisapp.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaseLawyerAssignmentRepository extends JpaRepository<CaseLawyerAssignment, Long> {

    // ========================================
    // BÚSQUEDAS POR CASO
    // ========================================

    /**
     * Buscar todas las asignaciones de un caso
     */
    List<CaseLawyerAssignment> findByLegalCaseId(Long caseId);

    /**
     * Buscar asignaciones por status de un caso
     */
    List<CaseLawyerAssignment> findByLegalCaseIdAndStatus(Long caseId, AssignmentStatus status);

    /**
     * Contar asignaciones de un caso
     */
    int countByLegalCaseId(Long caseId);

    /**
     * Contar asignaciones por status de un caso
     */
    int countByLegalCaseIdAndStatus(Long caseId, AssignmentStatus status);

    // ========================================
    // BÚSQUEDAS POR USUARIO (CORREGIDO)
    // ========================================

    /**
     * Buscar todas las asignaciones de un usuario (CORREGIDO: usar 'user')
     */
    @Query("SELECT cla FROM CaseLawyerAssignment cla WHERE cla.user.id = :userId")
    List<CaseLawyerAssignment> findByUserId(@Param("userId") Long userId);

    /**
     * Buscar asignaciones por status de un usuario (CORREGIDO: usar 'user')
     */
    @Query("SELECT cla FROM CaseLawyerAssignment cla WHERE cla.user.id = :userId AND cla.status = :status")
    List<CaseLawyerAssignment> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") AssignmentStatus status);

    /**
     * Buscar casos activos de un usuario (CORREGIDO: usar 'user' y enum)
     */
    @Query("SELECT cla FROM CaseLawyerAssignment cla WHERE cla.user.id = :userId AND cla.status = :status")
    List<CaseLawyerAssignment> findActiveCasesByUserId(@Param("userId") Long userId, @Param("status") AssignmentStatus status);

    /**
     * Contar casos por status de un usuario (CORREGIDO: usar 'user')
     */
    @Query("SELECT COUNT(cla) FROM CaseLawyerAssignment cla WHERE cla.user.id = :userId AND cla.status = :status")
    int countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") AssignmentStatus status);

    // ========================================
    // BÚSQUEDAS POR ESTADO
    // ========================================

    /**
     * Buscar todas las asignaciones por estado
     */
    List<CaseLawyerAssignment> findByStatus(AssignmentStatus status);

    /**
     * Buscar asignaciones por rol
     */
    List<CaseLawyerAssignment> findByRole(String role);

    // ========================================
    // BÚSQUEDAS POR FECHA
    // ========================================

    /**
     * Buscar asignaciones creadas en un rango de fechas
     */
    List<CaseLawyerAssignment> findByAssignedDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Buscar asignaciones sin confirmar después de X días (CORREGIDO: usar enum)
     */
    @Query("SELECT cla FROM CaseLawyerAssignment cla WHERE cla.assignedDate < :cutoffDate AND cla.status = :status")
    List<CaseLawyerAssignment> findUnconfirmedAssignmentsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate, @Param("status") AssignmentStatus status);

    // ========================================
    // OPERACIONES DE MODIFICACIÓN (CORREGIDAS)
    // ========================================

    /**
     * Eliminar asignación específica por caso y usuario (CORREGIDO: usar user_id)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CaseLawyerAssignment cla WHERE cla.legalCase.id = :caseId AND cla.user.id = :userId")
    void deleteByLegalCaseIdAndUserId(@Param("caseId") Long caseId, @Param("userId") Long userId);

    /**
     * Desactivar todas las asignaciones de un caso (CORREGIDO: usar enum)
     */
    @Modifying
    @Transactional
    @Query("UPDATE CaseLawyerAssignment cla SET cla.status = :inactiveStatus " +
            "WHERE cla.legalCase.id = :caseId AND cla.status = :activeStatus")
    void deactivateAllAssignmentsByCase(@Param("caseId") Long caseId,
                                        @Param("activeStatus") AssignmentStatus activeStatus,
                                        @Param("inactiveStatus") AssignmentStatus inactiveStatus);

    // ========================================
    // ESTADÍSTICAS Y REPORTES (CORREGIDAS)
    // ========================================

    /**
     * Contar casos sin asignar (CORREGIDO: usar enum)
     */
    @Query("SELECT COUNT(DISTINCT lc.id) FROM LegalCase lc WHERE lc.id NOT IN " +
            "(SELECT DISTINCT cla.legalCase.id FROM CaseLawyerAssignment cla WHERE cla.status = :status)")
    Integer countUnassignedCases(@Param("status") AssignmentStatus status);

    /**
     * Obtener estadísticas de carga por abogado (CORREGIDO: usar 'user' y enum)
     */
    @Query("SELECT u.id, u.firstName, u.lastName, u.email, COUNT(cla.id) as caseCount " +
            "FROM User u LEFT JOIN CaseLawyerAssignment cla ON u.id = cla.user.id AND cla.status = :status " +
            "WHERE u.active = true GROUP BY u.id, u.firstName, u.lastName, u.email")
    List<Object[]> getLawyerWorkloadStats(@Param("status") AssignmentStatus status);

    /**
     * Obtener top abogados por carga de trabajo (CORREGIDO: usar 'user' y enum)
     */
    @Query("SELECT u.id, u.firstName, u.lastName, COUNT(cla.id) as caseCount " +
            "FROM User u LEFT JOIN CaseLawyerAssignment cla ON u.id = cla.user.id AND cla.status = :status " +
            "WHERE u.active = true GROUP BY u.id, u.firstName, u.lastName " +
            "ORDER BY COUNT(cla.id) DESC")
    List<Object[]> getTopLawyersByWorkload(@Param("status") AssignmentStatus status);

    /**
     * Contar asignaciones por rol (CORREGIDO: usar enum como parámetro)
     */
    @Query("SELECT cla.role, COUNT(cla.id) FROM CaseLawyerAssignment cla WHERE cla.status = :status GROUP BY cla.role")
    List<Object[]> countAssignmentsByRole(@Param("status") AssignmentStatus status);

    /**
     * Obtener casos por especialidad requerida (CORREGIDO: usar enum)
     */
    @Query("SELECT lc.requiredSpecialty, COUNT(DISTINCT cla.legalCase.id) " +
            "FROM CaseLawyerAssignment cla JOIN cla.legalCase lc WHERE cla.status = :status " +
            "GROUP BY lc.requiredSpecialty")
    List<Object[]> countCasesBySpecialty(@Param("status") AssignmentStatus status);

    // ========================================
    // BÚSQUEDAS ESPECÍFICAS PARA VALIDACIONES (CORREGIDAS)
    // ========================================

    /**
     * Verificar si un caso tiene el mínimo de abogados requerido (CORREGIDO: usar enum)
     */
    @Query("SELECT COUNT(cla.id) >= :minLawyers FROM CaseLawyerAssignment cla " +
            "WHERE cla.legalCase.id = :caseId AND cla.status = :status")
    Boolean caseHasMinimumLawyers(@Param("caseId") Long caseId, @Param("minLawyers") int minLawyers, @Param("status") AssignmentStatus status);

    /**
     * Verificar si un abogado ya está asignado a un caso (CORREGIDO: usar user_id)
     */
    @Query("SELECT CASE WHEN COUNT(cla) > 0 THEN true ELSE false END FROM CaseLawyerAssignment cla " +
            "WHERE cla.legalCase.id = :caseId AND cla.user.id = :userId AND cla.status = :status")
    boolean existsByLegalCaseIdAndUserIdAndStatus(@Param("caseId") Long caseId,
                                                  @Param("userId") Long userId,
                                                  @Param("status") AssignmentStatus status);

    /**
     * Buscar abogado principal de un caso
     */
    Optional<CaseLawyerAssignment> findByLegalCaseIdAndRoleAndStatus(Long caseId, String role, AssignmentStatus status);

    // ========================================
    // MÉTODOS PARA DASHBOARD Y MÉTRICAS (CORREGIDOS)
    // ========================================

    /**
     * Obtener distribución de casos por país (CORREGIDO: usar enum)
     */
    @Query("SELECT lc.country, COUNT(DISTINCT cla.legalCase.id) " +
            "FROM CaseLawyerAssignment cla JOIN cla.legalCase lc WHERE cla.status = :status " +
            "GROUP BY lc.country")
    List<Object[]> getCaseDistributionByCountry(@Param("status") AssignmentStatus status);

    /**
     * Obtener casos asignados hoy (CORREGIDO: usar enum)
     */
    @Query("SELECT COUNT(cla.id) FROM CaseLawyerAssignment cla " +
            "WHERE DATE(cla.assignedDate) = CURRENT_DATE AND cla.status = :status")
    Integer countTodayAssignments(@Param("status") AssignmentStatus status);

    /**
     * Obtener promedio de abogados por caso (CORREGIDO: usar enum)
     */
    @Query("SELECT AVG(assignmentCount) FROM " +
            "(SELECT COUNT(cla.id) as assignmentCount FROM CaseLawyerAssignment cla " +
            "WHERE cla.status = :status GROUP BY cla.legalCase.id)")
    Double getAverageLawyersPerCase(@Param("status") AssignmentStatus status);

    // ========================================
    // MÉTODOS ADICIONALES ÚTILES
    // ========================================

    /**
     * Obtener todas las asignaciones activas
     */
    default List<CaseLawyerAssignment> findAllActive() {
        return findByStatus(AssignmentStatus.ACTIVE);
    }

    /**
     * Obtener asignaciones activas de un caso
     */
    default List<CaseLawyerAssignment> findActiveByCaseId(Long caseId) {
        return findByLegalCaseIdAndStatus(caseId, AssignmentStatus.ACTIVE);
    }

    /**
     * Obtener asignaciones activas de un usuario
     */
    default List<CaseLawyerAssignment> findActiveByUserId(Long userId) {
        return findByUserIdAndStatus(userId, AssignmentStatus.ACTIVE);
    }

    /**
     * Verificar si un usuario está asignado activamente a un caso
     */
    default boolean isUserActivelyAssigned(Long caseId, Long userId) {
        return existsByLegalCaseIdAndUserIdAndStatus(caseId, userId, AssignmentStatus.ACTIVE);
    }

    /**
     * Contar casos sin asignar (método de conveniencia)
     */
    default Integer countUnassignedCases() {
        return countUnassignedCases(AssignmentStatus.ACTIVE);
    }

    /**
     * Obtener estadísticas de workload (método de conveniencia)
     */
    default List<Object[]> getLawyerWorkloadStats() {
        return getLawyerWorkloadStats(AssignmentStatus.ACTIVE);
    }

    /**
     * Contar asignaciones por rol activas (método de conveniencia)
     */
    default List<Object[]> countAssignmentsByRole() {
        return countAssignmentsByRole(AssignmentStatus.ACTIVE);
    }

    /**
     * Contar asignaciones de hoy activas (método de conveniencia)
     */
    default Integer countTodayAssignments() {
        return countTodayAssignments(AssignmentStatus.ACTIVE);
    }

    /**
     * Promedio de abogados por caso activo (método de conveniencia)
     */
    default Double getAverageLawyersPerCase() {
        return getAverageLawyersPerCase(AssignmentStatus.ACTIVE);
    }
}