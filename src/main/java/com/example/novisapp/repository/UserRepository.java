package com.example.novisapp.repository;

import com.example.novisapp.entity.User;
import com.example.novisapp.entity.UserRole; // ✅ USAR SOLO LA VERSIÓN DE ENTITY

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ========================================
    // BÚSQUEDAS BÁSICAS
    // ========================================

    /**
     * Buscar usuario por email
     */
    Optional<User> findByEmail(String email);

    /**
     * Buscar usuarios por rol
     */
    List<User> findByRole(UserRole role);

    /**
     * Buscar usuarios activos
     */
    List<User> findByActiveTrue();

    /**
     * Buscar usuarios por estado activo/inactivo
     */
    List<User> findByActive(Boolean active);

    /**
     * Buscar usuarios activos por rol
     */
    List<User> findByRoleAndActiveTrue(UserRole role);

    /**
     * Buscar usuarios por país
     */
    List<User> findByCountry(String country);

    // ========================================
    // MÉTODOS PARA JWT
    // ========================================

    /**
     * Buscar por email y activo (para JWT authentication)
     */
    Optional<User> findByEmailAndActiveTrue(String email);

    /**
     * Buscar por ID y activo
     */
    Optional<User> findByIdAndActiveTrue(Long id);

    /**
     * Verificar si existe por email
     */
    boolean existsByEmail(String email);

    /**
     * Buscar usuarios activos ordenados por apellido
     */
    List<User> findByActiveTrueOrderByLastName();

    /**
     * Buscar por rol y activo ordenados por apellido
     */
    List<User> findByRoleAndActiveTrueOrderByLastName(UserRole role);

    // ========================================
    // MÉTODOS PARA TEAM DASHBOARD (CORREGIDOS)
    // ========================================

    /**
     * Obtener abogados disponibles - CORREGIDO para usar enum
     */
    @Query("SELECT u FROM User u WHERE u.role IN ('LAWYER', 'MANAGING_PARTNER') AND u.active = true")
    List<User> findAvailableLawyers();

    /**
     * Obtener abogados con poca carga de trabajo
     */
    @Query("SELECT u FROM User u WHERE u.role IN ('LAWYER', 'MANAGING_PARTNER') AND u.active = true " +
            "AND (u.currentWorkload IS NULL OR u.currentWorkload < 10)")
    List<User> findLawyersWithLowWorkload();

    /**
     * Buscar abogados por especialización
     */
    @Query("SELECT u FROM User u WHERE u.specialization LIKE %:specialization% AND u.active = true")
    List<User> findBySpecialization(@Param("specialization") String specialization);

    /**
     * Buscar abogados disponibles ordenados por carga de trabajo
     */
    @Query("SELECT u FROM User u WHERE u.role IN ('LAWYER', 'MANAGING_PARTNER') AND u.active = true " +
            "ORDER BY COALESCE(u.currentWorkload, 0) ASC")
    List<User> findAvailableLawyersOrderByWorkload();

    /**
     * Buscar abogados por país y rol
     */
    @Query("SELECT u FROM User u WHERE u.country = :country AND u.role = :role AND u.active = true")
    List<User> findByCountryAndRole(@Param("country") String country, @Param("role") UserRole role);

    // ========================================
    // ESTADÍSTICAS Y REPORTES
    // ========================================

    /**
     * Contar usuarios activos
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsers();

    /**
     * Contar usuarios activos por rol
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.active = true")
    long countActiveUsersByRole(@Param("role") UserRole role);

    /**
     * Obtener promedio de carga de trabajo
     */
    @Query("SELECT AVG(u.currentWorkload) FROM User u WHERE u.role IN ('LAWYER', 'MANAGING_PARTNER') AND u.active = true")
    Double getAverageWorkload();

    /**
     * Obtener usuarios con mayor carga de trabajo
     */
    @Query("SELECT u FROM User u WHERE u.role IN ('LAWYER', 'MANAGING_PARTNER') AND u.active = true " +
            "ORDER BY COALESCE(u.currentWorkload, 0) DESC")
    List<User> findTopWorkloadLawyers();

    /**
     * Contar abogados disponibles (con workload < 10)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role IN ('LAWYER', 'MANAGING_PARTNER') AND u.active = true " +
            "AND (u.currentWorkload IS NULL OR u.currentWorkload < 10)")
    long countAvailableLawyers();

    /**
     * Buscar abogados por rango de carga de trabajo
     */
    @Query("SELECT u FROM User u WHERE u.role IN ('LAWYER', 'MANAGING_PARTNER') AND u.active = true " +
            "AND u.currentWorkload BETWEEN :minWorkload AND :maxWorkload")
    List<User> findByWorkloadRange(@Param("minWorkload") Integer minWorkload, @Param("maxWorkload") Integer maxWorkload);

    // ========================================
    // MÉTODOS ADICIONALES REQUERIDOS
    // ========================================

    /**
     * Estadísticas de workload por país
     */
    @Query("SELECT u.country, COUNT(u), AVG(COALESCE(u.currentWorkload, 0)) " +
            "FROM User u WHERE u.role IN ('LAWYER', 'MANAGING_PARTNER') AND u.active = true " +
            "GROUP BY u.country")
    List<Object[]> getWorkloadStatsByCountry();

    /**
     * Buscar usuarios que pueden tomar más casos
     */
    @Query("SELECT u FROM User u WHERE u.role IN ('LAWYER', 'MANAGING_PARTNER') AND u.active = true " +
            "AND (u.currentWorkload IS NULL OR u.currentWorkload < :maxCases)")
    List<User> findUsersWithCapacity(@Param("maxCases") Integer maxCases);

    /**
     * Resumen de carga de trabajo por usuario
     */
    @Query("SELECT u.id, u.firstName, u.lastName, COALESCE(u.currentWorkload, 0) as workload " +
            "FROM User u WHERE u.role IN ('LAWYER', 'MANAGING_PARTNER') AND u.active = true " +
            "ORDER BY workload DESC")
    List<Object[]> getUserWorkloadSummary();

    /**
     * Buscar usuarios bloqueados
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND u.accountNonLocked = false")
    List<User> findLockedUsers();

    /**
     * Buscar usuarios por especialización (versión con contiene)
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND u.specialization LIKE %:specialization%")
    List<User> findBySpecializationContaining(@Param("specialization") String specialization);

    /**
     * Buscar usuarios por país y activos ordenados
     */
    List<User> findByCountryAndActiveTrueOrderByLastName(String country);

    /**
     * Buscar usuarios con carga de trabajo específica
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND u.currentWorkload = :workload")
    List<User> findByCurrentWorkload(@Param("workload") Integer workload);

    /**
     * Buscar usuarios con carga de trabajo menor a un valor
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND (u.currentWorkload IS NULL OR u.currentWorkload < :maxWorkload)")
    List<User> findByCurrentWorkloadLessThan(@Param("maxWorkload") Integer maxWorkload);

    /**
     * Estadísticas de carga de trabajo - Máximo
     */
    @Query("SELECT MAX(u.currentWorkload) FROM User u WHERE u.active = true")
    Integer getMaxWorkload();

    /**
     * Estadísticas de carga de trabajo - Mínimo
     */
    @Query("SELECT MIN(u.currentWorkload) FROM User u WHERE u.active = true AND u.currentWorkload IS NOT NULL")
    Integer getMinWorkload();

    /**
     * Buscar usuarios que iniciaron sesión recientemente
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND u.lastLogin >= :sinceDate ORDER BY u.lastLogin DESC")
    List<User> findRecentlyLoggedUsers(@Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Buscar usuarios que nunca han iniciado sesión
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND u.lastLogin IS NULL")
    List<User> findUsersWhoNeverLoggedIn();

    /**
     * Buscar por nombre o apellido (búsqueda parcial)
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<User> findByNameContaining(@Param("searchTerm") String searchTerm);

    /**
     * Buscar usuarios con intentos fallidos de login
     */
    @Query("SELECT u FROM User u WHERE u.active = true AND u.failedLoginAttempts > 0 ORDER BY u.failedLoginAttempts DESC")
    List<User> findUsersWithFailedAttempts();

    // ========================================
    // MÉTODOS DE COMPATIBILIDAD
    // ========================================

    /**
     * Contar usuarios activos (método alternativo)
     */
    long countByActiveTrue();

    /**
     * Contar usuarios por rol y activos (método alternativo)
     */
    long countByRoleAndActiveTrue(UserRole role);
}