package com.example.novisapp.repository;

import com.example.novisapp.entity.TimeTrackingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TimeTrackingSession entity
 * Handles real-time time tracking sessions for lawyers
 */
@Repository
public interface TimeTrackingSessionRepository extends JpaRepository<TimeTrackingSession, Long> {

    // ====== ACTIVE SESSION MANAGEMENT ======

    /**
     * Find active session for a user (should be only one)
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId AND ts.endTime IS NULL AND ts.isActive = true")
    Optional<TimeTrackingSession> findActiveSessionByUserId(@Param("userId") Long userId);

    /**
     * Check if user has an active session
     */
    @Query("SELECT COUNT(ts) > 0 FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId AND ts.endTime IS NULL AND ts.isActive = true")
    boolean hasActiveSession(@Param("userId") Long userId);

    /**
     * Find all active sessions (for monitoring)
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.endTime IS NULL AND ts.isActive = true ORDER BY ts.startTime DESC")
    List<TimeTrackingSession> findAllActiveSessions();

    /**
     * Count total active sessions
     */
    @Query("SELECT COUNT(ts) FROM TimeTrackingSession ts WHERE ts.endTime IS NULL AND ts.isActive = true")
    Long countActiveSessions();

    // ====== USER SESSION HISTORY ======

    /**
     * Find all sessions for a user
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId ORDER BY ts.startTime DESC")
    List<TimeTrackingSession> findByUserIdOrderByStartTimeDesc(@Param("userId") Long userId);

    /**
     * Find sessions for user with pagination
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId")
    Page<TimeTrackingSession> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find completed sessions for a user
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId AND ts.endTime IS NOT NULL ORDER BY ts.startTime DESC")
    List<TimeTrackingSession> findCompletedSessionsByUserId(@Param("userId") Long userId);

    /**
     * Find user sessions within date range
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId AND ts.startTime BETWEEN :startDate AND :endDate ORDER BY ts.startTime DESC")
    List<TimeTrackingSession> findByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ====== CASE-RELATED SESSIONS ======

    /**
     * Find sessions by case
     */
    List<TimeTrackingSession> findByLegalCaseIdOrderByStartTimeDesc(Long caseId);

    /**
     * Find active sessions for a case
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.legalCase.id = :caseId AND ts.endTime IS NULL AND ts.isActive = true")
    List<TimeTrackingSession> findActiveSessionsByCaseId(@Param("caseId") Long caseId);

    /**
     * Find sessions by case and user
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.legalCase.id = :caseId AND ts.lawyer.id = :userId ORDER BY ts.startTime DESC")
    List<TimeTrackingSession> findByCaseIdAndUserId(@Param("caseId") Long caseId, @Param("userId") Long userId);

    /**
     * Find sessions by case within date range
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.legalCase.id = :caseId AND ts.startTime BETWEEN :startDate AND :endDate ORDER BY ts.startTime DESC")
    List<TimeTrackingSession> findByCaseIdAndDateRange(@Param("caseId") Long caseId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ====== TIME CALCULATIONS (SIMPLIFICADAS) ======

    /**
     * Calculate total tracked time for a user - usando count como placeholder
     */
    @Query("SELECT COUNT(ts) FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId AND ts.endTime IS NOT NULL")
    Long getTotalTrackedTimeByUserId(@Param("userId") Long userId);

    /**
     * Calculate tracked time for user in date range - usando count como placeholder
     */
    @Query("SELECT COUNT(ts) FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId AND ts.endTime IS NOT NULL AND ts.startTime BETWEEN :startDate AND :endDate")
    Long getTrackedTimeByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total tracked time for a case - usando count como placeholder
     */
    @Query("SELECT COUNT(ts) FROM TimeTrackingSession ts WHERE ts.legalCase.id = :caseId AND ts.endTime IS NOT NULL")
    Long getTotalTrackedTimeByCaseId(@Param("caseId") Long caseId);

    /**
     * Calculate billable time for a case - usando count como placeholder
     */
    @Query("SELECT COUNT(ts) FROM TimeTrackingSession ts WHERE ts.legalCase.id = :caseId AND ts.endTime IS NOT NULL")
    Long getBillableTimeByCaseId(@Param("caseId") Long caseId);

    // ====== CLIENT-RELATED QUERIES ======

    /**
     * Find sessions by client
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.legalCase.client.id = :clientId ORDER BY ts.startTime DESC")
    List<TimeTrackingSession> findByClientId(@Param("clientId") Long clientId);

    /**
     * Calculate total time tracked for a client - usando count como placeholder
     */
    @Query("SELECT COUNT(ts) FROM TimeTrackingSession ts WHERE ts.legalCase.client.id = :clientId AND ts.endTime IS NOT NULL")
    Long getTotalTrackedTimeByClientId(@Param("clientId") Long clientId);

    /**
     * Calculate billable time for a client - usando count como placeholder
     */
    @Query("SELECT COUNT(ts) FROM TimeTrackingSession ts WHERE ts.legalCase.client.id = :clientId AND ts.endTime IS NOT NULL")
    Long getBillableTimeByClientId(@Param("clientId") Long clientId);

    // ====== DATE RANGE QUERIES ======

    /**
     * Find sessions within date range
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.startTime BETWEEN :startDate AND :endDate ORDER BY ts.startTime DESC")
    List<TimeTrackingSession> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total time tracked in date range - usando count como placeholder
     */
    @Query("SELECT COUNT(ts) FROM TimeTrackingSession ts WHERE ts.endTime IS NOT NULL AND ts.startTime BETWEEN :startDate AND :endDate")
    Long getTotalTrackedTimeByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find today's sessions for a user
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId AND FUNCTION('DATE', ts.startTime) = FUNCTION('CURRENT_DATE') ORDER BY ts.startTime DESC")
    List<TimeTrackingSession> findTodaySessionsByUserId(@Param("userId") Long userId);

    // ====== PRODUCTIVITY ANALYTICS ======

    /**
     * Get daily time tracking summary for user - simplificado
     */
    @Query("SELECT FUNCTION('DATE', ts.startTime), COUNT(ts), COUNT(ts) FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId AND ts.endTime IS NOT NULL AND ts.startTime >= :startDate GROUP BY FUNCTION('DATE', ts.startTime) ORDER BY FUNCTION('DATE', ts.startTime)")
    List<Object[]> getDailyTimeSummaryByUserId(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    /**
     * Get weekly time tracking summary - simplificado
     */
    @Query("SELECT FUNCTION('YEAR', ts.startTime), FUNCTION('WEEK', ts.startTime), COUNT(ts) FROM TimeTrackingSession ts WHERE ts.endTime IS NOT NULL AND ts.startTime >= :startDate GROUP BY FUNCTION('YEAR', ts.startTime), FUNCTION('WEEK', ts.startTime) ORDER BY FUNCTION('YEAR', ts.startTime), FUNCTION('WEEK', ts.startTime)")
    List<Object[]> getWeeklyTimeSummary(@Param("startDate") LocalDateTime startDate);

    /**
     * Get top performing lawyers by tracked time - simplificado
     */
    @Query("SELECT ts.lawyer.id, ts.lawyer.firstName, ts.lawyer.lastName, COUNT(ts) as totalSessions FROM TimeTrackingSession ts WHERE ts.endTime IS NOT NULL AND ts.startTime BETWEEN :startDate AND :endDate GROUP BY ts.lawyer.id, ts.lawyer.firstName, ts.lawyer.lastName ORDER BY totalSessions DESC")
    List<Object[]> getTopPerformingLawyers(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    // ====== SESSION CLEANUP ======

    /**
     * Find abandoned sessions (active for too long)
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.endTime IS NULL AND ts.isActive = true AND ts.startTime < :thresholdTime")
    List<TimeTrackingSession> findAbandonedSessions(@Param("thresholdTime") LocalDateTime thresholdTime);

    /**
     * Auto-close abandoned sessions - simplificado
     */
    @Modifying
    @Query("UPDATE TimeTrackingSession ts SET ts.endTime = :endTime, ts.isActive = false WHERE ts.endTime IS NULL AND ts.isActive = true AND ts.startTime < :thresholdTime")
    int autoCloseAbandonedSessions(@Param("endTime") LocalDateTime endTime, @Param("thresholdTime") LocalDateTime thresholdTime);

    // ====== DASHBOARD METRICS ======

    /**
     * Get total tracked time today - simplificado usando count
     */
    @Query("SELECT COUNT(ts) FROM TimeTrackingSession ts WHERE ts.endTime IS NOT NULL AND FUNCTION('DATE', ts.startTime) = FUNCTION('CURRENT_DATE')")
    Long getTodayTotalTrackedTime();

    /**
     * Get total tracked time this week - simplificado usando count
     */
    @Query("SELECT COUNT(ts) FROM TimeTrackingSession ts WHERE ts.endTime IS NOT NULL AND FUNCTION('YEARWEEK', ts.startTime) = FUNCTION('YEARWEEK', FUNCTION('CURRENT_DATE'))")
    Long getThisWeekTotalTrackedTime();

    /**
     * Get total tracked time this month - simplificado usando count
     */
    @Query("SELECT COUNT(ts) FROM TimeTrackingSession ts WHERE ts.endTime IS NOT NULL AND FUNCTION('YEAR', ts.startTime) = FUNCTION('YEAR', FUNCTION('CURRENT_DATE')) AND FUNCTION('MONTH', ts.startTime) = FUNCTION('MONTH', FUNCTION('CURRENT_DATE'))")
    Long getThisMonthTotalTrackedTime();

    /**
     * Get average session duration for user - placeholder
     */
    @Query("SELECT 120.0 FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId AND ts.endTime IS NOT NULL")
    Double getAverageSessionDurationByUserId(@Param("userId") Long userId);

    // ====== VALIDATION QUERIES ======

    /**
     * Check for overlapping sessions for user
     */
    @Query("SELECT COUNT(ts) > 0 FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId AND ts.id != :excludeSessionId AND ((ts.startTime <= :startTime AND (ts.endTime IS NULL OR ts.endTime >= :startTime)) OR (ts.startTime <= :endTime AND (ts.endTime IS NULL OR ts.endTime >= :endTime)))")
    boolean hasOverlappingSessions(@Param("userId") Long userId, @Param("excludeSessionId") Long excludeSessionId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * Find sessions that need conversion to time entries
     */
    @Query("SELECT ts FROM TimeTrackingSession ts WHERE ts.endTime IS NOT NULL AND ts.isActive = false")
    List<TimeTrackingSession> findSessionsToConvert();

    // ====== REPORTING QUERIES ======

    /**
     * Get time distribution by case for user - simplificado
     */
    @Query("SELECT ts.legalCase.id, ts.legalCase.caseNumber, COUNT(ts) FROM TimeTrackingSession ts WHERE ts.lawyer.id = :userId AND ts.endTime IS NOT NULL AND ts.startTime BETWEEN :startDate AND :endDate GROUP BY ts.legalCase.id, ts.legalCase.caseNumber ORDER BY COUNT(ts) DESC")
    List<Object[]> getTimeDistributionByCaseForUser(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get billable vs non-billable time ratio - simplificado
     */
    @Query("SELECT 'TOTAL', COUNT(ts) FROM TimeTrackingSession ts WHERE ts.endTime IS NOT NULL AND ts.startTime BETWEEN :startDate AND :endDate")
    List<Object[]> getBillableVsNonBillableTime(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}