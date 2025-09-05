package com.example.novisapp.controller;

import com.example.novisapp.dto.*;
import com.example.novisapp.service.FinancialService;
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
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para gestión financiera
 * Expone endpoints para dashboard, configuración de casos, tiempo y gastos
 */
@RestController
@RequestMapping("/api/financial")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Financial Management", description = "APIs para gestión financiera del sistema legal")
@SecurityRequirement(name = "Bearer Authentication")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FinancialController {

    private final FinancialService financialService;

    // ====== DASHBOARD FINANCIERO ======

    @GetMapping("/dashboard")
    @Operation(
            summary = "Obtener dashboard financiero",
            description = "Retorna métricas financieras completas incluyendo ingresos, gastos, tiempo facturado y KPIs"
    )
    @ApiResponse(responseCode = "200", description = "Dashboard obtenido exitosamente")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<FinancialDashboardDTO> getFinancialDashboard() {
        log.info("Solicitud de dashboard financiero recibida");

        try {
            FinancialDashboardDTO dashboard = financialService.getFinancialDashboard();
            log.info("Dashboard financiero generado exitosamente");
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Error al generar dashboard financiero: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar dashboard financiero", e);
        }
    }

    @GetMapping("/dashboard/revenue-trend")
    @Operation(
            summary = "Obtener tendencia de ingresos",
            description = "Retorna datos históricos de ingresos para gráficos de tendencias"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<List<MonthlyRevenueDTO>> getRevenueTrend(
            @Parameter(description = "Número de meses a incluir") @RequestParam(defaultValue = "12") int months) {

        log.info("Solicitud de tendencia de ingresos para {} meses", months);

        try {
            List<MonthlyRevenueDTO> trend = financialService.getRevenueHistory(months);
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            log.error("Error al obtener tendencia de ingresos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener tendencia de ingresos", e);
        }
    }

    @GetMapping("/dashboard/top-clients")
    @Operation(
            summary = "Obtener clientes principales por ingresos",
            description = "Retorna lista de clientes que generan más ingresos"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<List<TopRevenueClientDTO>> getTopRevenueClients(
            @Parameter(description = "Número de clientes a retornar") @RequestParam(defaultValue = "10") int limit) {

        log.info("Solicitud de top {} clientes por ingresos", limit);

        try {
            List<TopRevenueClientDTO> topClients = financialService.getTopRevenueClients(limit);
            return ResponseEntity.ok(topClients);
        } catch (Exception e) {
            log.error("Error al obtener top clientes: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener top clientes", e);
        }
    }

    @GetMapping("/dashboard/top-lawyers")
    @Operation(
            summary = "Obtener abogados principales por rendimiento",
            description = "Retorna lista de abogados con mejor rendimiento financiero"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<List<TopPerformingLawyerDTO>> getTopPerformingLawyers(
            @Parameter(description = "Número de abogados a retornar") @RequestParam(defaultValue = "10") int limit) {

        log.info("Solicitud de top {} abogados por rendimiento", limit);

        try {
            List<TopPerformingLawyerDTO> topLawyers = financialService.getTopPerformingLawyers(limit);
            return ResponseEntity.ok(topLawyers);
        } catch (Exception e) {
            log.error("Error al obtener top abogados: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener top abogados", e);
        }
    }

    // ====== CONFIGURACIÓN FINANCIERA DE CASOS ======

    @PostMapping("/cases")
    @Operation(
            summary = "Crear configuración financiera para caso",
            description = "Establece la configuración de facturación para un caso legal"
    )
    @ApiResponse(responseCode = "201", description = "Configuración creada exitosamente")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<FinancialCaseDTO> createFinancialCase(@Valid @RequestBody FinancialCaseDTO financialCaseDTO) {
        log.info("Creando configuración financiera para caso ID: {}", financialCaseDTO.getLegalCaseId());

        try {
            FinancialCaseDTO created = financialService.createFinancialCase(financialCaseDTO);
            log.info("Configuración financiera creada con ID: {}", created.getId());
            return ResponseEntity.status(201).body(created);
        } catch (Exception e) {
            log.error("Error al crear configuración financiera: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear configuración financiera", e);
        }
    }

    @GetMapping("/cases/{caseId}")
    @Operation(
            summary = "Obtener configuración financiera de caso",
            description = "Retorna la configuración financiera de un caso específico"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER') or hasRole('LAWYER')")
    public ResponseEntity<FinancialCaseDTO> getFinancialCase(
            @Parameter(description = "ID del caso legal") @PathVariable Long caseId) {

        log.info("Obteniendo configuración financiera para caso ID: {}", caseId);

        try {
            FinancialCaseDTO financialCase = financialService.getFinancialCaseByCaseId(caseId);
            if (financialCase == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(financialCase);
        } catch (Exception e) {
            log.error("Error al obtener configuración financiera: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener configuración financiera", e);
        }
    }

    @PutMapping("/cases/{id}")
    @Operation(
            summary = "Actualizar configuración financiera",
            description = "Actualiza la configuración financiera de un caso"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<FinancialCaseDTO> updateFinancialCase(
            @Parameter(description = "ID de la configuración financiera") @PathVariable Long id,
            @Valid @RequestBody FinancialCaseDTO financialCaseDTO) {

        log.info("Actualizando configuración financiera ID: {}", id);

        try {
            FinancialCaseDTO updated = financialService.updateFinancialCase(id, financialCaseDTO);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error al actualizar configuración financiera: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar configuración financiera", e);
        }
    }

    // ====== GESTIÓN DE ENTRADAS DE TIEMPO ======

    @PostMapping("/time-entries")
    @Operation(
            summary = "Crear entrada de tiempo",
            description = "Registra tiempo trabajado en un caso"
    )
    @ApiResponse(responseCode = "201", description = "Entrada de tiempo creada exitosamente")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<TimeEntryDTO> createTimeEntry(@Valid @RequestBody TimeEntryDTO timeEntryDTO) {
        log.info("Creando entrada de tiempo para caso ID: {}", timeEntryDTO.getLegalCaseId());

        try {
            TimeEntryDTO created = financialService.createTimeEntry(timeEntryDTO);
            log.info("Entrada de tiempo creada con ID: {}", created.getId());
            return ResponseEntity.status(201).body(created);
        } catch (Exception e) {
            log.error("Error al crear entrada de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear entrada de tiempo", e);
        }
    }

    @PostMapping("/time-entries/convert-session/{sessionId}")
    @Operation(
            summary = "Convertir sesión de tracking a entrada de tiempo",
            description = "Convierte una sesión de tiempo rastreado en una entrada de tiempo facturable"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<TimeEntryDTO> convertSessionToTimeEntry(
            @Parameter(description = "ID de la sesión de tracking") @PathVariable Long sessionId,
            @Parameter(description = "Descripción del trabajo realizado") @RequestParam(required = false) String description) {

        log.info("Convirtiendo sesión de tracking ID: {} a entrada de tiempo", sessionId);

        try {
            TimeEntryDTO timeEntry = financialService.convertSessionToTimeEntry(sessionId, description);
            return ResponseEntity.ok(timeEntry);
        } catch (Exception e) {
            log.error("Error al convertir sesión a entrada de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al convertir sesión a entrada de tiempo", e);
        }
    }

    @PutMapping("/time-entries/{id}/approve")
    @Operation(
            summary = "Aprobar entrada de tiempo",
            description = "Aprueba una entrada de tiempo para facturación"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<TimeEntryDTO> approveTimeEntry(
            @Parameter(description = "ID de la entrada de tiempo") @PathVariable Long id,
            @Parameter(description = "ID del revisor") @RequestParam Long reviewerId) {

        log.info("Aprobando entrada de tiempo ID: {}", id);

        try {
            TimeEntryDTO approved = financialService.approveTimeEntry(id, reviewerId);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            log.error("Error al aprobar entrada de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al aprobar entrada de tiempo", e);
        }
    }

    @PutMapping("/time-entries/{id}/reject")
    @Operation(
            summary = "Rechazar entrada de tiempo",
            description = "Rechaza una entrada de tiempo con razón"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<TimeEntryDTO> rejectTimeEntry(
            @Parameter(description = "ID de la entrada de tiempo") @PathVariable Long id,
            @Parameter(description = "ID del revisor") @RequestParam Long reviewerId,
            @Parameter(description = "Razón del rechazo") @RequestParam String reason) {

        log.info("Rechazando entrada de tiempo ID: {}", id);

        try {
            TimeEntryDTO rejected = financialService.rejectTimeEntry(id, reviewerId, reason);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            log.error("Error al rechazar entrada de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al rechazar entrada de tiempo", e);
        }
    }

    // ====== GESTIÓN DE GASTOS ======

    @PostMapping("/expenses")
    @Operation(
            summary = "Crear gasto de caso",
            description = "Registra un gasto asociado a un caso legal"
    )
    @ApiResponse(responseCode = "201", description = "Gasto creado exitosamente")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('LAWYER') or hasRole('PARALEGAL')")
    public ResponseEntity<CaseExpenseDTO> createExpense(@Valid @RequestBody CaseExpenseDTO expenseDTO) {
        log.info("Creando gasto para caso ID: {}", expenseDTO.getLegalCaseId());

        try {
            CaseExpenseDTO created = financialService.createExpense(expenseDTO);
            log.info("Gasto creado con ID: {}", created.getId());
            return ResponseEntity.status(201).body(created);
        } catch (Exception e) {
            log.error("Error al crear gasto: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear gasto", e);
        }
    }

    @PutMapping("/expenses/{id}/approve")
    @Operation(
            summary = "Aprobar gasto",
            description = "Aprueba un gasto para reembolso o facturación"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<CaseExpenseDTO> approveExpense(
            @Parameter(description = "ID del gasto") @PathVariable Long id,
            @Parameter(description = "ID del revisor") @RequestParam Long reviewerId) {

        log.info("Aprobando gasto ID: {}", id);

        try {
            CaseExpenseDTO approved = financialService.approveExpense(id, reviewerId);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            log.error("Error al aprobar gasto: {}", e.getMessage(), e);
            throw new RuntimeException("Error al aprobar gasto", e);
        }
    }

    // ====== REPORTES Y ANÁLISIS ======

    @GetMapping("/reports/profitability")
    @Operation(
            summary = "Reporte de rentabilidad por caso",
            description = "Retorna análisis de rentabilidad de casos"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<List<CaseProfitabilityDTO>> getCaseProfitabilityReport(
            @Parameter(description = "Fecha de inicio") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fecha de fin") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Generando reporte de rentabilidad desde {} hasta {}", startDate, endDate);

        try {
            List<CaseProfitabilityDTO> report = financialService.getCaseProfitabilityReport(startDate, endDate);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error al generar reporte de rentabilidad: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar reporte de rentabilidad", e);
        }
    }

    @GetMapping("/reports/expenses-summary")
    @Operation(
            summary = "Resumen de gastos por categoría",
            description = "Retorna resumen de gastos agrupados por categoría"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<List<ExpenseSummaryDTO>> getExpensesSummary(
            @Parameter(description = "Fecha de inicio") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fecha de fin") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Generando resumen de gastos desde {} hasta {}", startDate, endDate);

        try {
            List<ExpenseSummaryDTO> summary = financialService.getExpensesSummary(startDate, endDate);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error al generar resumen de gastos: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar resumen de gastos", e);
        }
    }

    @GetMapping("/reports/time-analytics")
    @Operation(
            summary = "Análisis de tiempo trabajado",
            description = "Retorna métricas detalladas de tiempo trabajado y facturado"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<TimeAnalyticsDTO> getTimeAnalytics(
            @Parameter(description = "ID del usuario (opcional)") @RequestParam(required = false) Long userId,
            @Parameter(description = "Fecha de inicio") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fecha de fin") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Generando análisis de tiempo para usuario {} desde {} hasta {}", userId, startDate, endDate);

        try {
            TimeAnalyticsDTO analytics = financialService.getTimeAnalytics(userId, startDate, endDate);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("Error al generar análisis de tiempo: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar análisis de tiempo", e);
        }
    }

    // ====== ENDPOINTS DE UTILIDAD ======

    @GetMapping("/cases/{caseId}/summary")
    @Operation(
            summary = "Resumen financiero de caso",
            description = "Retorna resumen completo financiero de un caso específico"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER') or hasRole('LAWYER')")
    public ResponseEntity<CaseFinancialSummaryDTO> getCaseFinancialSummary(
            @Parameter(description = "ID del caso") @PathVariable Long caseId) {

        log.info("Obteniendo resumen financiero para caso ID: {}", caseId);

        try {
            CaseFinancialSummaryDTO summary = financialService.getCaseFinancialSummary(caseId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error al obtener resumen financiero de caso: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener resumen financiero de caso", e);
        }
    }

    @GetMapping("/billing/pending")
    @Operation(
            summary = "Obtener elementos pendientes de facturación",
            description = "Retorna tiempo y gastos aprobados pendientes de facturación"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER') or hasRole('FINANCIAL_MANAGER')")
    public ResponseEntity<PendingBillingDTO> getPendingBilling() {
        log.info("Obteniendo elementos pendientes de facturación");

        try {
            PendingBillingDTO pendingBilling = financialService.getPendingBilling();
            return ResponseEntity.ok(pendingBilling);
        } catch (Exception e) {
            log.error("Error al obtener elementos pendientes de facturación: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener elementos pendientes de facturación", e);
        }
    }

    // ====== MANEJO DE ERRORES ======

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("Error en controlador financiero: {}", e.getMessage(), e);
        ErrorResponse error = new ErrorResponse(
                "FINANCIAL_ERROR",
                e.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // DTO para respuestas de error
    public record ErrorResponse(String code, String message, LocalDateTime timestamp) {}
}