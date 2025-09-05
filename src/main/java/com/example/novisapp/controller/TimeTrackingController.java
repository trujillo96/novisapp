package com.example.novisapp.controller;

import com.example.novisapp.dto.*;
import com.example.novisapp.service.TimeTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para gestión de seguimiento de tiempo
 * Maneja cronómetro en tiempo real, sesiones activas y conversión a entradas de tiempo
 */
@RestController
@RequestMapping("/api/time-tracking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Time Tracking", description = "APIs para seguimiento de tiempo en tiempo real")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TimeTrackingController {

    private final TimeTrackingService timeTrackingService;

    // ====== GESTIÓN DE SESIONES ACTIVAS ======

    @PostMapping("/sessions/start")
    @Operation(
            summary = "Iniciar sesión de seguimiento de tiempo",
            description = "Inicia el cronómetro para rastrear tiempo en un caso específico"
    )
    @ApiResponse(responseCode = "201", description = "Sesión iniciada exitosamente")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<TimeTrackingSessionDTO> startTimeTracking(
            @Valid @RequestBody StartTimeTrackingRequest request,
            Authentication auth) {

        log.info("Iniciando sesión de tiempo para usuario: {} en caso: {}",
                auth.getName(), request.caseId());

        try {
            TimeTrackingSessionDTO session = timeTrackingService.startTimeTracking(
                    request.caseId(),
                    request.userId(),
                    request.description(),
                    request.isBillable()
            );

            log.info("Sesión de tiempo iniciada con ID: {}", session.getId());
            return ResponseEntity.status(201).body(session);
        } catch (Exception e) {
            log.error("Error al iniciar sesión de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al iniciar sesión de tiempo", e);
        }
    }

    @PutMapping("/sessions/{sessionId}/stop")
    @Operation(
            summary = "Detener sesión de seguimiento de tiempo",
            description = "Detiene el cronómetro y calcula el tiempo total trabajado"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<TimeTrackingSessionDTO> stopTimeTracking(
            @Parameter(description = "ID de la sesión") @PathVariable Long sessionId,
            @RequestBody(required = false) StopTimeTrackingRequest request,
            Authentication auth) {

        log.info("Deteniendo sesión de tiempo ID: {} por usuario: {}", sessionId, auth.getName());

        try {
            String notes = request != null ? request.notes() : null;
            TimeTrackingSessionDTO session = timeTrackingService.stopTimeTracking(sessionId, notes);

            log.info("Sesión de tiempo detenida. Tiempo total: {} minutos", session.getTotalMinutes());
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            log.error("Error al detener sesión de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al detener sesión de tiempo", e);
        }
    }

    @PutMapping("/sessions/{sessionId}/pause")
    @Operation(
            summary = "Pausar sesión de seguimiento de tiempo",
            description = "Pausa temporalmente el cronómetro sin detenerlo completamente"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<TimeTrackingSessionDTO> pauseTimeTracking(
            @Parameter(description = "ID de la sesión") @PathVariable Long sessionId,
            Authentication auth) {

        log.info("Pausando sesión de tiempo ID: {} por usuario: {}", sessionId, auth.getName());

        try {
            TimeTrackingSessionDTO session = timeTrackingService.pauseTimeTracking(sessionId);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            log.error("Error al pausar sesión de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al pausar sesión de tiempo", e);
        }
    }

    @PutMapping("/sessions/{sessionId}/resume")
    @Operation(
            summary = "Reanudar sesión de seguimiento de tiempo",
            description = "Reanuda una sesión de tiempo pausada"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<TimeTrackingSessionDTO> resumeTimeTracking(
            @Parameter(description = "ID de la sesión") @PathVariable Long sessionId,
            Authentication auth) {

        log.info("Reanudando sesión de tiempo ID: {} por usuario: {}", sessionId, auth.getName());

        try {
            TimeTrackingSessionDTO session = timeTrackingService.resumeTimeTracking(sessionId);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            log.error("Error al reanudar sesión de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al reanudar sesión de tiempo", e);
        }
    }

    // ====== CONSULTA DE SESIONES ======

    @GetMapping("/sessions/active")
    @Operation(
            summary = "Obtener sesiones activas",
            description = "Retorna todas las sesiones de tiempo actualmente en progreso"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<List<ActiveSessionDTO>> getActiveSessions() {
        log.info("Obteniendo sesiones activas de tiempo");

        try {
            List<ActiveSessionDTO> activeSessions = timeTrackingService.getActiveSessions();
            return ResponseEntity.ok(activeSessions);
        } catch (Exception e) {
            log.error("Error al obtener sesiones activas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener sesiones activas", e);
        }
    }

    @GetMapping("/sessions/user/{userId}/active")
    @Operation(
            summary = "Obtener sesión activa del usuario",
            description = "Retorna la sesión activa de un usuario específico (si existe)"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<TimeTrackingSessionDTO> getUserActiveSession(
            @Parameter(description = "ID del usuario") @PathVariable Long userId,
            Authentication auth) {

        log.info("Obteniendo sesión activa para usuario ID: {}", userId);

        try {
            TimeTrackingSessionDTO activeSession = timeTrackingService.getUserActiveSession(userId);
            if (activeSession == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(activeSession);
        } catch (Exception e) {
            log.error("Error al obtener sesión activa del usuario: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener sesión activa del usuario", e);
        }
    }

    @GetMapping("/sessions/user/{userId}")
    @Operation(
            summary = "Obtener historial de sesiones del usuario",
            description = "Retorna el historial de sesiones de tiempo de un usuario con paginación"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<Page<TimeTrackingSessionDTO>> getUserSessions(
            @Parameter(description = "ID del usuario") @PathVariable Long userId,
            @Parameter(description = "Fecha de inicio") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fecha de fin") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable,
            Authentication auth) {

        log.info("Obteniendo historial de sesiones para usuario ID: {}", userId);

        try {
            Page<TimeTrackingSessionDTO> sessions = timeTrackingService.getUserSessions(
                    userId, startDate, endDate, pageable);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            log.error("Error al obtener historial de sesiones: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener historial de sesiones", e);
        }
    }

    @GetMapping("/sessions/case/{caseId}")
    @Operation(
            summary = "Obtener sesiones por caso",
            description = "Retorna todas las sesiones de tiempo asociadas a un caso específico"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<List<TimeTrackingSessionDTO>> getCaseSessions(
            @Parameter(description = "ID del caso") @PathVariable Long caseId,
            @Parameter(description = "Fecha de inicio") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fecha de fin") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Obteniendo sesiones para caso ID: {}", caseId);

        try {
            List<TimeTrackingSessionDTO> sessions = timeTrackingService.getCaseSessions(
                    caseId, startDate, endDate);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            log.error("Error al obtener sesiones del caso: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener sesiones del caso", e);
        }
    }

    // ====== TIEMPO REAL Y ESTADO ======

    @GetMapping("/sessions/{sessionId}/current-time")
    @Operation(
            summary = "Obtener tiempo actual de sesión",
            description = "Retorna el tiempo transcurrido en tiempo real de una sesión activa"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<CurrentTimeDTO> getCurrentSessionTime(
            @Parameter(description = "ID de la sesión") @PathVariable Long sessionId) {

        try {
            CurrentTimeDTO currentTime = timeTrackingService.getCurrentSessionTime(sessionId);
            return ResponseEntity.ok(currentTime);
        } catch (Exception e) {
            log.error("Error al obtener tiempo actual de sesión: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener tiempo actual de sesión", e);
        }
    }

    @GetMapping("/dashboard/real-time")
    @Operation(
            summary = "Dashboard en tiempo real",
            description = "Retorna métricas de tiempo en tiempo real para el dashboard"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<RealTimeTrackingDashboardDTO> getRealTimeDashboard() {
        log.info("Obteniendo dashboard de tiempo en tiempo real");

        try {
            RealTimeTrackingDashboardDTO dashboard = timeTrackingService.getRealTimeDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error al obtener dashboard en tiempo real: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener dashboard en tiempo real", e);
        }
    }

    // ====== CONVERSIÓN Y GESTIÓN ======

    @PostMapping("/sessions/{sessionId}/convert-to-entry")
    @Operation(
            summary = "Convertir sesión a entrada de tiempo",
            description = "Convierte una sesión completada en una entrada de tiempo facturable"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<TimeEntryDTO> convertSessionToTimeEntry(
            @Parameter(description = "ID de la sesión") @PathVariable Long sessionId,
            @RequestBody ConvertSessionRequest request,
            Authentication auth) {

        log.info("Convirtiendo sesión ID: {} a entrada de tiempo por usuario: {}",
                sessionId, auth.getName());

        try {
            TimeEntryDTO timeEntry = timeTrackingService.convertSessionToTimeEntry(
                    sessionId,
                    request.description(),
                    request.overrideBillableMinutes()
            );

            log.info("Sesión convertida a entrada de tiempo con ID: {}", timeEntry.getId());
            return ResponseEntity.ok(timeEntry);
        } catch (Exception e) {
            log.error("Error al convertir sesión a entrada de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al convertir sesión a entrada de tiempo", e);
        }
    }

    @GetMapping("/sessions/unconverted")
    @Operation(
            summary = "Obtener sesiones no convertidas",
            description = "Retorna sesiones completadas que aún no han sido convertidas a entradas de tiempo"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<List<TimeTrackingSessionDTO>> getUnconvertedSessions(
            @Parameter(description = "ID del usuario (opcional)") @RequestParam(required = false) Long userId) {

        log.info("Obteniendo sesiones no convertidas para usuario: {}", userId);

        try {
            List<TimeTrackingSessionDTO> sessions = timeTrackingService.getUnconvertedSessions(userId);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            log.error("Error al obtener sesiones no convertidas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener sesiones no convertidas", e);
        }
    }

    // ====== REPORTES Y ANALYTICS ======

    @GetMapping("/analytics/user/{userId}")
    @Operation(
            summary = "Análisis de tiempo por usuario",
            description = "Retorna métricas detalladas de tiempo trabajado por un usuario específico"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<UserTimeAnalyticsDTO> getUserTimeAnalytics(
            @Parameter(description = "ID del usuario") @PathVariable Long userId,
            @Parameter(description = "Fecha de inicio") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fecha de fin") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Generando análisis de tiempo para usuario ID: {}", userId);

        try {
            UserTimeAnalyticsDTO analytics = timeTrackingService.getUserTimeAnalytics(
                    userId, startDate, endDate);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error al generar análisis de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar análisis de tiempo", e);
        }
    }

    @GetMapping("/analytics/productivity")
    @Operation(
            summary = "Reporte de productividad",
            description = "Retorna métricas de productividad y utilización de tiempo"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<ProductivityReportDTO> getProductivityReport(
            @Parameter(description = "Fecha de inicio") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fecha de fin") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Generando reporte de productividad desde {} hasta {}", startDate, endDate);

        try {
            ProductivityReportDTO report = timeTrackingService.getProductivityReport(startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error al generar reporte de productividad: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar reporte de productividad", e);
        }
    }

    // ====== ADMINISTRACIÓN ======

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(
            summary = "Eliminar sesión de tiempo",
            description = "Elimina una sesión de tiempo (solo para administradores)"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<Void> deleteSession(
            @Parameter(description = "ID de la sesión") @PathVariable Long sessionId,
            Authentication auth) {

        log.info("Eliminando sesión de tiempo ID: {} por administrador: {}", sessionId, auth.getName());

        try {
            timeTrackingService.deleteSession(sessionId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error al eliminar sesión de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al eliminar sesión de tiempo", e);
        }
    }

    @PostMapping("/sessions/cleanup-abandoned")
    @Operation(
            summary = "Limpiar sesiones abandonadas",
            description = "Cierra automáticamente sesiones abandonadas (más de 24 horas activas)"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<CleanupResultDTO> cleanupAbandonedSessions(Authentication auth) {
        log.info("Iniciando limpieza de sesiones abandonadas por: {}", auth.getName());

        try {
            CleanupResultDTO result = timeTrackingService.cleanupAbandonedSessions();
            log.info("Limpieza completada: {} sesiones cerradas", result.getClosedSessions());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error en limpieza de sesiones abandonadas: {}", e.getMessage(), e);
            throw new RuntimeException("Error en limpieza de sesiones abandonadas", e);
        }
    }

    // ====== DTOs DE REQUEST ======

    public record StartTimeTrackingRequest(
            Long caseId,
            Long userId,
            String description,
            Boolean isBillable
    ) {}

    public record StopTimeTrackingRequest(
            String notes
    ) {}

    public record ConvertSessionRequest(
            String description,
            Integer overrideBillableMinutes
    ) {}

    // ====== MANEJO DE ERRORES ======

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("Error en controlador de time tracking: {}", e.getMessage(), e);
        ErrorResponse error = new ErrorResponse(
                "TIME_TRACKING_ERROR",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    public record ErrorResponse(String code, String message, LocalDateTime timestamp) {}
}