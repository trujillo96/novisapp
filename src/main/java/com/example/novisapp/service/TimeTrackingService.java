package com.example.novisapp.service;

import com.example.novisapp.dto.*;
import com.example.novisapp.entity.*;
import com.example.novisapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de seguimiento de tiempo en tiempo real
 * Maneja cronómetros, sesiones activas y conversión a entradas de tiempo
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TimeTrackingService {

    private final TimeTrackingSessionRepository timeTrackingSessionRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final UserRepository userRepository;
    private final FinancialService financialService;

    // ====== GESTIÓN DE SESIONES ======

    /**
     * Inicia una nueva sesión de seguimiento de tiempo
     */
    public TimeTrackingSessionDTO startTimeTracking(Long caseId, Long userId, String description, Boolean isBillable) {
        log.info("Iniciando sesión de tiempo para usuario {} en caso {}", userId, caseId);

        // Verificar que no tenga una sesión activa
        if (timeTrackingSessionRepository.hasActiveSession(userId)) {
            throw new RuntimeException("El usuario ya tiene una sesión activa. Debe detenerla antes de iniciar una nueva.");
        }

        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Caso legal no encontrado"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        TimeTrackingSession session = TimeTrackingSession.builder()
                .legalCase(legalCase)
                .lawyer(user)
                .description(description)
                .startTime(LocalDateTime.now())
                .isActive(true)
                // ✅ NO HAY CAMPO isBillable/billable en el builder - se manejará después
                .build();

        TimeTrackingSession saved = timeTrackingSessionRepository.save(session);
        log.info("Sesión de tiempo iniciada con ID: {}", saved.getId());

        return convertToTimeTrackingSessionDTO(saved);
    }

    /**
     * Detiene una sesión de seguimiento de tiempo
     */
    public TimeTrackingSessionDTO stopTimeTracking(Long sessionId, String notes) {
        log.info("Deteniendo sesión de tiempo ID: {}", sessionId);

        TimeTrackingSession session = timeTrackingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión de tiempo no encontrada"));

        if (!session.getIsActive()) {
            throw new RuntimeException("La sesión ya está detenida");
        }

        // ✅ Usar el método stop() de la entidad
        session.stop();

        // ✅ NO existe setSessionNotes - no hay campo notes en la entidad
        // Las notas se manejarán en la descripción si es necesario

        TimeTrackingSession updated = timeTrackingSessionRepository.save(session);
        log.info("Sesión detenida. Tiempo total: {} minutos", session.getTotalMinutes());

        return convertToTimeTrackingSessionDTO(updated);
    }

    /**
     * Pausa una sesión de tiempo (implementación futura)
     */
    public TimeTrackingSessionDTO pauseTimeTracking(Long sessionId) {
        // Para implementación futura - pausar/reanudar sesiones
        throw new RuntimeException("Funcionalidad de pausa no implementada aún");
    }

    /**
     * Reanuda una sesión pausada (implementación futura)
     */
    public TimeTrackingSessionDTO resumeTimeTracking(Long sessionId) {
        // Para implementación futura - pausar/reanudar sesiones
        throw new RuntimeException("Funcionalidad de reanudación no implementada aún");
    }

    // ====== CONSULTAS DE SESIONES ======

    /**
     * Obtiene todas las sesiones activas
     */
    public List<ActiveSessionDTO> getActiveSessions() {
        List<TimeTrackingSession> activeSessions = timeTrackingSessionRepository.findAllActiveSessions();

        return activeSessions.stream()
                .map(this::convertToActiveSessionDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene la sesión activa de un usuario
     */
    public TimeTrackingSessionDTO getUserActiveSession(Long userId) {
        return timeTrackingSessionRepository.findActiveSessionByUserId(userId)
                .map(this::convertToTimeTrackingSessionDTO)
                .orElse(null);
    }

    /**
     * Obtiene el historial de sesiones de un usuario
     */
    public Page<TimeTrackingSessionDTO> getUserSessions(Long userId, LocalDateTime startDate,
                                                        LocalDateTime endDate, Pageable pageable) {
        List<TimeTrackingSession> sessions;

        if (startDate != null && endDate != null) {
            sessions = timeTrackingSessionRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        } else {
            sessions = timeTrackingSessionRepository.findByUserIdOrderByStartTimeDesc(userId);
        }

        List<TimeTrackingSessionDTO> sessionDTOs = sessions.stream()
                .map(this::convertToTimeTrackingSessionDTO)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sessionDTOs.size());

        return new PageImpl<>(
                sessionDTOs.subList(start, end),
                pageable,
                sessionDTOs.size()
        );
    }

    /**
     * Obtiene sesiones por caso
     */
    public List<TimeTrackingSessionDTO> getCaseSessions(Long caseId, LocalDateTime startDate, LocalDateTime endDate) {
        List<TimeTrackingSession> sessions;

        if (startDate != null && endDate != null) {
            sessions = timeTrackingSessionRepository.findByCaseIdAndDateRange(caseId, startDate, endDate);
        } else {
            sessions = timeTrackingSessionRepository.findByLegalCaseIdOrderByStartTimeDesc(caseId);
        }

        return sessions.stream()
                .map(this::convertToTimeTrackingSessionDTO)
                .collect(Collectors.toList());
    }

    // ====== TIEMPO REAL ======

    /**
     * Obtiene el tiempo actual de una sesión activa
     */
    public CurrentTimeDTO getCurrentSessionTime(Long sessionId) {
        TimeTrackingSession session = timeTrackingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        if (!session.getIsActive()) {
            throw new RuntimeException("La sesión no está activa");
        }

        long currentMinutes = session.getTotalMinutes(); // ✅ Usar método de la entidad
        String formattedTime = session.getElapsedTimeFormatted(); // ✅ Usar método de la entidad

        return CurrentTimeDTO.builder()
                .sessionId(sessionId)
                .currentMinutes(currentMinutes)
                .formattedTime(formattedTime)
                .isActive(session.getIsActive())
                .startTime(session.getStartTime())
                .build();
    }

    /**
     * Dashboard en tiempo real
     */
    public RealTimeTrackingDashboardDTO getRealTimeDashboard() {
        List<TimeTrackingSession> activeSessions = timeTrackingSessionRepository.findAllActiveSessions();

        long totalActiveMinutes = activeSessions.stream()
                .mapToLong(TimeTrackingSession::getTotalMinutes) // ✅ Usar método de la entidad
                .sum();

        long todayTrackedMinutes = timeTrackingSessionRepository.getTodayTotalTrackedTime();
        long weekTrackedMinutes = timeTrackingSessionRepository.getThisWeekTotalTrackedTime();
        long monthTrackedMinutes = timeTrackingSessionRepository.getThisMonthTotalTrackedTime();

        return RealTimeTrackingDashboardDTO.builder()
                .activeSessionsCount(activeSessions.size())
                .totalActiveMinutes(totalActiveMinutes)
                .todayTrackedMinutes(todayTrackedMinutes)
                .weekTrackedMinutes(weekTrackedMinutes)
                .monthTrackedMinutes(monthTrackedMinutes)
                .activeSessions(activeSessions.stream()
                        .map(this::convertToActiveSessionDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    // ====== CONVERSIÓN ======

    /**
     * Convierte sesión a entrada de tiempo
     */
    public TimeEntryDTO convertSessionToTimeEntry(Long sessionId, String description, Integer overrideBillableMinutes) {
        TimeTrackingSession session = timeTrackingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // ✅ NO existe getConverted - verificar si puede convertirse usando el método de la entidad
        if (!session.canConvertToTimeEntry()) {
            throw new RuntimeException("La sesión no puede convertirse a entrada de tiempo");
        }

        if (session.getIsActive()) {
            throw new RuntimeException("No se puede convertir una sesión activa. Debe detenerla primero.");
        }

        // Usar el servicio financiero para crear la entrada de tiempo
        TimeEntryDTO timeEntryDTO = TimeEntryDTO.builder()
                .legalCaseId(session.getLegalCase().getId())
                .userId(session.getLawyer().getId())
                .description(description != null ? description : session.getDescription())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .totalMinutes((int) session.getTotalMinutes()) // ✅ Cast a int
                .billableMinutes(overrideBillableMinutes != null ? overrideBillableMinutes :
                        (session.isBillable() ? (int) session.getTotalMinutes() : 0)) // ✅ Usar método isBillable()
                .isBillable(session.isBillable()) // ✅ Usar método isBillable()
                .build();

        TimeEntryDTO created = financialService.createTimeEntry(timeEntryDTO);

        // ✅ NO existe setConverted - no marcar como convertida por ahora
        // Esto se podría manejar con un campo adicional si es necesario

        return created;
    }

    /**
     * Obtiene sesiones no convertidas
     */
    public List<TimeTrackingSessionDTO> getUnconvertedSessions(Long userId) {
        List<TimeTrackingSession> sessions = timeTrackingSessionRepository.findSessionsToConvert();

        if (userId != null) {
            final Long finalUserId = userId;
            sessions = sessions.stream()
                    .filter(session -> session.getLawyer().getId().equals(finalUserId))
                    .collect(Collectors.toList());
        }

        return sessions.stream()
                .map(this::convertToTimeTrackingSessionDTO)
                .collect(Collectors.toList());
    }

    // ====== ANALYTICS ======

    /**
     * Analytics de tiempo por usuario
     */
    public UserTimeAnalyticsDTO getUserTimeAnalytics(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        final LocalDateTime finalStartDate = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        final LocalDateTime finalEndDate = endDate != null ? endDate : LocalDateTime.now();

        Long totalMinutes = timeTrackingSessionRepository.getTrackedTimeByUserIdAndDateRange(userId, finalStartDate, finalEndDate);
        List<Object[]> dailySummary = timeTrackingSessionRepository.getDailyTimeSummaryByUserId(userId, finalStartDate);
        Double avgSessionDuration = timeTrackingSessionRepository.getAverageSessionDurationByUserId(userId);

        return UserTimeAnalyticsDTO.builder()
                .userId(userId)
                .totalMinutes(totalMinutes != null ? totalMinutes : 0L)
                .totalHours(totalMinutes != null ? totalMinutes / 60.0 : 0.0)
                .averageSessionDuration(avgSessionDuration != null ? avgSessionDuration : 0.0)
                .period(finalStartDate.format(DateTimeFormatter.ISO_DATE) + " to " + finalEndDate.format(DateTimeFormatter.ISO_DATE))
                .build();
    }

    /**
     * Reporte de productividad
     */
    public ProductivityReportDTO getProductivityReport(LocalDateTime startDate, LocalDateTime endDate) {
        final LocalDateTime finalStartDate = startDate != null ? startDate : LocalDateTime.now().minusMonths(1);
        final LocalDateTime finalEndDate = endDate != null ? endDate : LocalDateTime.now();

        List<Object[]> topLawyers = timeTrackingSessionRepository.getTopPerformingLawyers(finalStartDate, finalEndDate,
                org.springframework.data.domain.PageRequest.of(0, 10));

        List<Object[]> billableVsNonBillable = timeTrackingSessionRepository.getBillableVsNonBillableTime(finalStartDate, finalEndDate);

        return ProductivityReportDTO.builder()
                .periodStart(finalStartDate)
                .periodEnd(finalEndDate)
                .totalLawyers((int) userRepository.countActiveUsersByRole(UserRole.LAWYER))
                .topPerformers(topLawyers.size())
                .build();
    }

    // ====== ADMINISTRACIÓN ======

    /**
     * Elimina una sesión
     */
    public void deleteSession(Long sessionId) {
        TimeTrackingSession session = timeTrackingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // ✅ Usar método de la entidad para verificar si puede eliminarse
        if (!session.canDelete()) {
            throw new RuntimeException("No se puede eliminar esta sesión");
        }

        timeTrackingSessionRepository.delete(session);
        log.info("Sesión de tiempo eliminada: {}", sessionId);
    }

    /**
     * Limpia sesiones abandonadas
     */
    public CleanupResultDTO cleanupAbandonedSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        int closedSessions = timeTrackingSessionRepository.autoCloseAbandonedSessions(
                LocalDateTime.now(), threshold);

        return CleanupResultDTO.builder()
                .closedSessions(closedSessions)
                .cleanupTime(LocalDateTime.now())
                .message("Limpieza completada: " + closedSessions + " sesiones cerradas automáticamente")
                .build();
    }

    // ====== MÉTODOS DE CONVERSIÓN ======

    private TimeTrackingSessionDTO convertToTimeTrackingSessionDTO(TimeTrackingSession entity) {
        Long currentDurationMinutes = null;
        if (entity.getIsActive()) {
            currentDurationMinutes = entity.getTotalMinutes(); // ✅ Usar método de la entidad
        }

        return TimeTrackingSessionDTO.builder()
                .id(entity.getId())
                .userId(entity.getLawyer().getId())
                .userName(entity.getLawyer().getFirstName() + " " + entity.getLawyer().getLastName())
                .legalCaseId(entity.getLegalCase().getId())
                .caseNumber(entity.getLegalCase().getCaseNumber())
                .caseTitle(entity.getLegalCase().getTitle())
                .description(entity.getDescription())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .totalMinutes((int) entity.getTotalMinutes()) // ✅ Cast a int
                .isActive(entity.getIsActive())
                .isBillable(entity.isBillable()) // ✅ Usar método isBillable()
                .notes(entity.getDescription()) // ✅ No hay campo notes, usar description
                .convertedToTimeEntry(false) // ✅ Por defecto false ya que no existe el campo
                .createdAt(entity.getCreatedAt())
                .currentDurationMinutes(currentDurationMinutes)
                .build();
    }

    private ActiveSessionDTO convertToActiveSessionDTO(TimeTrackingSession entity) {
        long currentMinutes = (long) entity.getTotalMinutes(); // ✅ Cast a long

        return ActiveSessionDTO.builder()
                .sessionId(entity.getId())
                .userId(entity.getLawyer().getId())
                .userName(entity.getLawyer().getFirstName() + " " + entity.getLawyer().getLastName())
                .caseId(entity.getLegalCase().getId())
                .caseNumber(entity.getLegalCase().getCaseNumber())
                .caseTitle(entity.getLegalCase().getTitle())
                .clientName(entity.getLegalCase().getClient() != null ?
                        entity.getLegalCase().getClient().getCompany() : "Sin cliente")
                .startTime(entity.getStartTime())
                .currentMinutes(currentMinutes)
                .description(entity.getDescription())
                .isBillable(entity.isBillable()) // ✅ Usar método isBillable()
                .status(entity.getIsActive() ? "Active" : "Stopped")
                .build();
    }

    // ====== MÉTODOS HELPER ======

    private String formatDuration(long minutes) {
        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }
}