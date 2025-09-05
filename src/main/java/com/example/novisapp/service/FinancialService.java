package com.example.novisapp.service;

import com.example.novisapp.dto.*;
import com.example.novisapp.entity.*;
import com.example.novisapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio principal para gestión financiera del sistema legal
 * Maneja facturación, tiempo, gastos y reportes financieros
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FinancialService {

    private final FinancialCaseRepository financialCaseRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final CaseExpenseRepository caseExpenseRepository;
    private final TimeTrackingSessionRepository timeTrackingSessionRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    // ====== DASHBOARD PRINCIPAL ======

    public FinancialDashboardDTO getFinancialDashboard() {
        log.info("Generando dashboard financiero completo");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDateTime endOfLastMonth = startOfMonth.minusSeconds(1);
        LocalDateTime startOfYear = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);

        return FinancialDashboardDTO.builder()
                .totalRevenue(calculateTotalRevenue())
                .monthlyRevenue(calculateMonthlyRevenue(startOfMonth, now))
                .revenueGrowth(calculateRevenueGrowth(startOfMonth, now, startOfLastMonth, endOfLastMonth))
                .totalExpenses(calculateTotalExpenses(startOfYear, now))
                .profitMargin(calculateProfitMargin(startOfMonth, now))
                .totalTrackedHours(calculateTotalTrackedHours())
                .billableHours(calculateBillableHours(startOfMonth, now))
                .utilizationRate(calculateUtilizationRate(startOfMonth, now))
                .activeTimeSessions(countActiveTimeSessions())
                .totalActiveCases(countActiveCases())
                .casesRequiringBilling(countCasesRequiringBilling())
                .overdueInvoices(countOverdueInvoices())
                .activeLawyers(countActiveLawyers())
                .lawyersTracking(countLawyersCurrentlyTracking())
                .pendingTimeEntries(countPendingTimeEntries())
                .pendingExpenses(countPendingExpenses())
                .pendingReimbursements(calculatePendingReimbursements())
                .revenueHistory(getRevenueHistory(12))
                .topClients(getTopRevenueClients(5))
                .topLawyers(getTopPerformingLawyers(5))
                .billingSummary(getBillingSummary(startOfMonth, now))
                .build();
    }
    // ====== MÉTODOS PÚBLICOS PARA REPORTES ======

    public List<MonthlyRevenueDTO> getRevenueHistory(int months) {
        log.info("Obteniendo historial de ingresos para {} meses", months);

        List<MonthlyRevenueDTO> history = new ArrayList<>();
        LocalDateTime currentDate = LocalDateTime.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDateTime monthStart = currentDate.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

            BigDecimal revenue = calculateMonthlyRevenue(monthStart, monthEnd);
            BigDecimal expenses = calculateTotalExpenses(monthStart, monthEnd);
            BigDecimal profit = revenue.subtract(expenses);

            String monthYear = monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            history.add(MonthlyRevenueDTO.builder()
                    .month(monthStart.getMonth().toString())
                    .year(monthStart.getYear())
                    .monthYear(monthYear)
                    .revenue(revenue)
                    .expenses(expenses)
                    .profit(profit)
                    .billableHours(calculateBillableHours(monthStart, monthEnd))
                    .activeCases(countActiveCasesInPeriod(monthStart, monthEnd))
                    .growthRate(BigDecimal.ZERO)
                    .build());
        }

        return history;
    }

    public List<TopRevenueClientDTO> getTopRevenueClients(int limit) {
        log.info("Obteniendo top {} clientes por ingresos", limit);

        try {
            // Datos simulados mejorados porque las entidades no están completamente mapeadas
            List<TopRevenueClientDTO> topClients = new ArrayList<>();

            for (int i = 1; i <= limit; i++) {
                topClients.add(TopRevenueClientDTO.builder()
                        .clientId((long) i)
                        .clientName("Cliente " + i)
                        .companyName("Empresa " + i + " S.A.")
                        .totalRevenue(BigDecimal.valueOf(50000 + (i * 10000)))
                        .totalBilled(BigDecimal.valueOf(45000 + (i * 9000)))
                        .totalBillableHours((long) (200 + (i * 50)))
                        .activeCases(3 + i)
                        .completedCases(i * 2)
                        .averageCaseValue(BigDecimal.valueOf(15000 + (i * 2000)))
                        .lastActivityDate(LocalDateTime.now().minusDays(i).format(DateTimeFormatter.ISO_DATE))
                        .build());
            }

            return topClients;
        } catch (Exception e) {
            log.warn("Error al obtener datos de clientes, retornando lista vacía: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<TopPerformingLawyerDTO> getTopPerformingLawyers(int limit) {
        log.info("Obteniendo top {} abogados por rendimiento", limit);

        try {
            LocalDate startDate = LocalDate.now().minusMonths(3);
            LocalDate endDate = LocalDate.now();

            List<Object[]> lawyerStats = timeEntryRepository.findTopRevenueGeneratingLawyers(
                    startDate, endDate, PageRequest.of(0, limit)).getContent();

            if (lawyerStats.isEmpty()) {
                // Datos simulados si no hay datos reales
                return generateSimulatedTopLawyers(limit);
            }

            return lawyerStats.stream()
                    .map(data -> {
                        Long userId = (Long) data[0];
                        String firstName = (String) data[1];
                        String lastName = (String) data[2];
                        BigDecimal totalRevenue = (BigDecimal) data[3];

                        return TopPerformingLawyerDTO.builder()
                                .lawyerId(userId)
                                .firstName(firstName)
                                .lastName(lastName)
                                .fullName(firstName + " " + lastName)
                                .totalBillableHours(calculateBillableHoursForLawyer(userId))
                                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                                .averageHourlyRate(calculateAverageHourlyRate(userId))
                                .activeCases(countActiveCasesByLawyer(userId))
                                .utilizationRate(calculateLawyerUtilization(userId, startDate.atStartOfDay(), endDate.atStartOfDay()))
                                .timeEntriesCount(countTimeEntriesByLawyer(userId))
                                .specialization("General")
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Error al obtener datos de abogados, retornando lista simulada: {}", e.getMessage());
            return generateSimulatedTopLawyers(limit);
        }
    }

    private List<TopPerformingLawyerDTO> generateSimulatedTopLawyers(int limit) {
        List<TopPerformingLawyerDTO> topLawyers = new ArrayList<>();
        String[] nombres = {"Dr. María", "Dr. Carlos", "Dra. Ana", "Dr. Luis", "Dra. Carmen"};
        String[] apellidos = {"González", "Rodríguez", "Martínez", "López", "García"};
        String[] especialidades = {"Litigación", "Corporativo", "Penal", "Laboral", "Comercial"};

        for (int i = 1; i <= limit; i++) {
            topLawyers.add(TopPerformingLawyerDTO.builder()
                    .lawyerId((long) i)
                    .firstName(nombres[i % nombres.length])
                    .lastName(apellidos[i % apellidos.length] + " " + i)
                    .fullName(nombres[i % nombres.length] + " " + apellidos[i % apellidos.length] + " " + i)
                    .totalBillableHours((long) (150 + (i * 20)))
                    .totalRevenue(BigDecimal.valueOf(75000 + (i * 15000)))
                    .averageHourlyRate(BigDecimal.valueOf(500 + (i * 50)))
                    .activeCases(5 + i)
                    .utilizationRate(75.0 + (i * 2))
                    .timeEntriesCount(50 + (i * 10))
                    .specialization(especialidades[i % especialidades.length])
                    .build());
        }
        return topLawyers;
    }
    // ====== CONFIGURACIÓN FINANCIERA DE CASOS ======

    public FinancialCaseDTO createFinancialCase(FinancialCaseDTO dto) {
        log.info("Creando configuración financiera para caso ID: {}", dto.getLegalCaseId());

        LegalCase legalCase = legalCaseRepository.findById(dto.getLegalCaseId())
                .orElseThrow(() -> new RuntimeException("Caso legal no encontrado"));

        FinancialCase financialCase = FinancialCase.builder()
                .legalCase(legalCase)
                .billingType(dto.getBillingType())
                .hourlyRate(dto.getHourlyRate())
                .fixedFee(dto.getFixedFee())
                .contingencyPercentage(dto.getContingencyPercentage())
                .retainerAmount(dto.getRetainerAmount())
                .budgetLimit(dto.getBudgetLimit())
                // QUITAR: .billingEnabled(dto.getBillingEnabled() != null ? dto.getBillingEnabled() : true)
                .billingNotes(dto.getBillingNotes())
                .build();

        FinancialCase saved = financialCaseRepository.save(financialCase);
        return convertToFinancialCaseDTO(saved);
    }

    public FinancialCaseDTO updateFinancialCase(Long id, FinancialCaseDTO dto) {
        FinancialCase existing = financialCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuración financiera no encontrada"));

        existing.setBillingType(dto.getBillingType());
        existing.setHourlyRate(dto.getHourlyRate());
        existing.setFixedFee(dto.getFixedFee());
        existing.setContingencyPercentage(dto.getContingencyPercentage());
        existing.setRetainerAmount(dto.getRetainerAmount());
        existing.setBudgetLimit(dto.getBudgetLimit());
        // QUITAR: existing.setBillingEnabled(dto.getBillingEnabled());
        existing.setBillingNotes(dto.getBillingNotes());

        FinancialCase updated = financialCaseRepository.save(existing);
        return convertToFinancialCaseDTO(updated);
    }

    public FinancialCaseDTO getFinancialCaseByCaseId(Long caseId) {
        return financialCaseRepository.findByLegalCaseId(caseId)
                .map(this::convertToFinancialCaseDTO)
                .orElse(null);
    }
    // ====== GESTIÓN DE TIEMPO ======

    public TimeEntryDTO createTimeEntry(TimeEntryDTO dto) {
        LegalCase legalCase = legalCaseRepository.findById(dto.getLegalCaseId())
                .orElseThrow(() -> new RuntimeException("Caso legal no encontrado"));

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Integer totalMinutes = dto.getTotalMinutes();
        if (totalMinutes == null && dto.getStartTime() != null && dto.getEndTime() != null) {
            totalMinutes = (int) java.time.Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes();
        }

        BigDecimal hourlyRate = dto.getHourlyRate();
        if (hourlyRate == null) {
            hourlyRate = getHourlyRateForCase(dto.getLegalCaseId());
        }

        TimeEntry timeEntry = TimeEntry.builder()
                .legalCase(legalCase)
                .lawyer(user)
                .description(dto.getDescription())
                .workDate(dto.getStartTime() != null ? dto.getStartTime().toLocalDate() : LocalDate.now())
                .duration(totalMinutes != null ? BigDecimal.valueOf(totalMinutes / 60.0) : BigDecimal.ZERO)
                .hourlyRate(hourlyRate)
                .billable(dto.getIsBillable() != null ? dto.getIsBillable() : true)
                .status(TimeEntryStatus.DRAFT)  // ✅ DRAFT SÍ EXISTE
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (timeEntry.getDuration() != null && hourlyRate != null) {
            timeEntry.setTotalAmount(timeEntry.getDuration().multiply(hourlyRate));
        }

        TimeEntry saved = timeEntryRepository.save(timeEntry);
        log.info("Entrada de tiempo creada con ID: {}", saved.getId());

        return convertToTimeEntryDTO(saved);
    }



    public TimeEntryDTO convertSessionToTimeEntry(Long sessionId, String description) {
        log.info("Convirtiendo sesión de tracking ID: {} a entrada de tiempo", sessionId);

        TimeTrackingSession session = timeTrackingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión de tracking no encontrada"));

        if (session.getEndTime() == null) {
            throw new RuntimeException("La sesión debe estar completada antes de convertirla");
        }

        // Crear DTO para la nueva entrada de tiempo
        TimeEntryDTO dto = TimeEntryDTO.builder()
                .legalCaseId(session.getLegalCase().getId())
                .userId(session.getLawyer().getId()) // ✅ USAR getLawyer()
                .description(description != null ? description : session.getDescription())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .totalMinutes((int) session.getTotalMinutes())
                .billableMinutes(session.isBillable() ? (int) session.getTotalMinutes() : 0)
                .isBillable(session.isBillable())
                .build();

        TimeEntryDTO created = createTimeEntry(dto);
        timeTrackingSessionRepository.save(session);

        log.info("Sesión convertida exitosamente a entrada de tiempo con ID: {}", created.getId());
        return created;
    }

    public TimeEntryDTO approveTimeEntry(Long timeEntryId, Long reviewerId) {
        log.info("Aprobando entrada de tiempo ID: {} por revisor ID: {}", timeEntryId, reviewerId);

        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new RuntimeException("Entrada de tiempo no encontrada"));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Revisor no encontrado"));

        // Verificar que la entrada esté en estado correcto para aprobación
        if (timeEntry.getStatus() != TimeEntryStatus.SUBMITTED) {
            throw new RuntimeException("Solo se pueden aprobar entradas de tiempo en estado SUBMITTED");
        }

        // Actualizar estado y datos de aprobación
        timeEntry.setStatus(TimeEntryStatus.APPROVED);
        timeEntry.setUpdatedAt(LocalDateTime.now());

        // Si el entity tiene campos de revisión, agregarlos aquí
        // timeEntry.setReviewedBy(reviewer);
        // timeEntry.setReviewedAt(LocalDateTime.now());

        TimeEntry updated = timeEntryRepository.save(timeEntry);
        log.info("Entrada de tiempo aprobada exitosamente");

        return convertToTimeEntryDTO(updated);
    }

    public TimeEntryDTO rejectTimeEntry(Long timeEntryId, Long reviewerId, String reason) {
        log.info("Rechazando entrada de tiempo ID: {} por revisor ID: {}", timeEntryId, reviewerId);

        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new RuntimeException("Entrada de tiempo no encontrada"));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Revisor no encontrado"));

        // Verificar que la entrada esté en estado correcto para rechazo
        if (timeEntry.getStatus() != TimeEntryStatus.SUBMITTED) {
            throw new RuntimeException("Solo se pueden rechazar entradas de tiempo en estado SUBMITTED");
        }

        // Validar que se proporcione una razón de rechazo
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("Debe proporcionarse una razón para el rechazo");
        }

        // Actualizar estado y datos de rechazo
        timeEntry.setStatus(TimeEntryStatus.REJECTED);
        timeEntry.setUpdatedAt(LocalDateTime.now());

        // Si el entity tiene campos de revisión y rechazo, agregarlos aquí
        // timeEntry.setReviewedBy(reviewer);
        // timeEntry.setReviewedAt(LocalDateTime.now());
        // timeEntry.setRejectionReason(reason);

        TimeEntry updated = timeEntryRepository.save(timeEntry);
        log.info("Entrada de tiempo rechazada exitosamente");

        return convertToTimeEntryDTO(updated);
    }

    // ====== MÉTODOS HELPER PARA GESTIÓN DE TIEMPO ======

    /**
     * Obtiene la tarifa por hora configurada para un caso específico
     */
    private BigDecimal getHourlyRateForCase(Long caseId) {
        try {
            return financialCaseRepository.findByLegalCaseId(caseId)
                    .map(FinancialCase::getHourlyRate)
                    .orElse(BigDecimal.valueOf(600)); // Tarifa por defecto
        } catch (Exception e) {
            log.warn("Error al obtener tarifa por hora para caso {}, usando tarifa por defecto", caseId);
            return BigDecimal.valueOf(600);
        }
    }

    /**
     * Calcula las horas facturables para un abogado específico
     */
    private Long calculateBillableHoursForLawyer(Long lawyerId) {
        try {
            LocalDate startDate = LocalDate.now().minusMonths(3);
            LocalDate endDate = LocalDate.now();

            // Verificar que el método existe en tu repository
            List<TimeEntry> entries = timeEntryRepository.findByLawyerIdOrderByWorkDateDesc(lawyerId);

            return entries.stream()
                    .filter(entry -> entry.getBillable() &&
                            entry.getWorkDate().isAfter(startDate.minusDays(1)) &&
                            entry.getWorkDate().isBefore(endDate.plusDays(1)))
                    .map(entry -> entry.getDuration() != null ? entry.getDuration().longValue() : 0L)
                    .reduce(0L, Long::sum);
        } catch (Exception e) {
            log.warn("Error al calcular horas facturables para abogado {}: {}", lawyerId, e.getMessage());
            return 0L;
        }
    }

    private BigDecimal calculateAverageHourlyRate(Long lawyerId) {
        try {
            // Verificar que el método existe
            List<TimeEntry> entries = timeEntryRepository.findByLawyerIdOrderByWorkDateDesc(lawyerId);

            if (entries.isEmpty()) {
                return BigDecimal.valueOf(600); // Valor por defecto
            }

            BigDecimal totalRate = entries.stream()
                    .filter(entry -> entry.getHourlyRate() != null)
                    .map(TimeEntry::getHourlyRate)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return totalRate.divide(BigDecimal.valueOf(entries.size()), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Error al calcular tarifa promedio para abogado {}: {}", lawyerId, e.getMessage());
            return BigDecimal.valueOf(600);
        }
    }

    private Integer countActiveCasesByLawyer(Long lawyerId) {
        try {
            User lawyer = userRepository.findById(lawyerId).orElse(null);
            if (lawyer != null && lawyer.getAssignedCases() != null) {
                return (int) lawyer.getAssignedCases().stream()
                        .filter(legalCase -> legalCase.getStatus() == CaseStatus.OPEN ||
                                legalCase.getStatus() == CaseStatus.IN_PROGRESS)
                        .count();
            }
            return 0;
        } catch (Exception e) {
            log.warn("Error al contar casos activos para abogado {}: {}", lawyerId, e.getMessage());
            return 0;
        }
    }

    /**
     * Calcula la tasa de utilización de un abogado en un período
     */
    private Double calculateLawyerUtilization(Long lawyerId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // Lógica para calcular utilización (horas facturables / horas totales)
            LocalDate start = startDate.toLocalDate();
            LocalDate end = endDate.toLocalDate();

            Optional<BigDecimal> totalHours = timeEntryRepository.getTotalHoursByLawyer(lawyerId, start, end);
            // Asumiendo que las horas facturables son un porcentaje de las totales
            return totalHours.map(hours -> Math.min(85.0, hours.doubleValue() * 0.75)).orElse(0.0);
        } catch (Exception e) {
            log.warn("Error al calcular utilización para abogado {}: {}", lawyerId, e.getMessage());
            return 75.0; // Valor por defecto
        }
    }

    /**
     * Cuenta las entradas de tiempo registradas por un abogado
     */
    private Integer countTimeEntriesByLawyer(Long lawyerId) {
        try {
            List<TimeEntry> entries = timeEntryRepository.findByLawyerIdOrderByWorkDateDesc(lawyerId);
            return entries.size();
        } catch (Exception e) {
            log.warn("Error al contar entradas de tiempo para abogado {}: {}", lawyerId, e.getMessage());
            return 0;
        }
    }
    // ====== GESTIÓN DE GASTOS ======

    public CaseExpenseDTO createExpense(CaseExpenseDTO dto) {
        log.info("Creando gasto para caso ID: {}", dto.getLegalCaseId());

        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto del gasto debe ser mayor a cero");
        }

        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            throw new RuntimeException("La descripción del gasto es obligatoria");
        }

        LegalCase legalCase = legalCaseRepository.findById(dto.getLegalCaseId())
                .orElseThrow(() -> new RuntimeException("Caso legal no encontrado"));

        User submitter = userRepository.findById(dto.getSubmittedById())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Crear el gasto
        CaseExpense expense = CaseExpense.builder()
                .legalCase(legalCase)
                .category(dto.getCategory())
                .description(dto.getDescription())
                .amount(dto.getAmount())
                // CORRECCIÓN: CONVERTIR LocalDateTime a LocalDate
                .expenseDate(dto.getExpenseDate() != null ?
                        dto.getExpenseDate().toLocalDate() : LocalDate.now())
                .receiptNumber(dto.getReceiptNumber())
                .receiptUrl(dto.getReceiptPath()) // USAR receiptUrl NO receiptPath
                .billableToClient(dto.getBillableToClient() != null ? dto.getBillableToClient() : false)
                .status(ExpenseStatus.PENDING)
                .createdByUser(submitter) // USAR createdByUser NO submittedBy
                .build();

        // Validar duplicados si se proporciona número de recibo
        if (expense.getReceiptNumber() != null && !expense.getReceiptNumber().trim().isEmpty()) {
            boolean isDuplicate = caseExpenseRepository.existsByLegalCaseIdAndReceiptNumber(
                    expense.getLegalCase().getId(),
                    expense.getReceiptNumber()
            );

            if (isDuplicate) {
                throw new RuntimeException("Ya existe un gasto con el número de recibo: " + expense.getReceiptNumber());
            }
        }

        CaseExpense saved = caseExpenseRepository.save(expense);
        log.info("Gasto creado con ID: {}", saved.getId());

        return convertToCaseExpenseDTO(saved);
    }
    public CaseExpenseDTO approveExpense(Long expenseId, Long reviewerId) {
        log.info("Aprobando gasto ID: {} por revisor ID: {}", expenseId, reviewerId);

        CaseExpense expense = caseExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Revisor no encontrado"));

        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new RuntimeException("Solo se pueden aprobar gastos en estado PENDIENTE");
        }

        validateBudgetLimits(expense);

        // USAR approvedByUser NO reviewedBy
        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setApprovedByUser(reviewer);
        expense.setApprovedAt(LocalDateTime.now());

        CaseExpense updated = caseExpenseRepository.save(expense);
        log.info("Gasto aprobado exitosamente");

        return convertToCaseExpenseDTO(updated);
    }

    public CaseExpenseDTO rejectExpense(Long expenseId, Long reviewerId, String reason) {
        log.info("Rechazando gasto ID: {} por revisor ID: {}", expenseId, reviewerId);

        CaseExpense expense = caseExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("Revisor no encontrado"));

        // Verificar que el gasto esté en estado correcto para rechazo
        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new RuntimeException("Solo se pueden rechazar gastos en estado PENDIENTE");
        }

        // Validar que se proporcione una razón de rechazo
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("Debe proporcionarse una razón para el rechazo");
        }

        // Actualizar estado y datos de rechazo
        expense.setStatus(ExpenseStatus.REJECTED);
      //  expense.setReviewedBy(reviewer);
     //   expense.setReviewedAt(LocalDateTime.now());
        expense.setRejectionReason(reason);

        CaseExpense updated = caseExpenseRepository.save(expense);
        log.info("Gasto rechazado exitosamente");

        return convertToCaseExpenseDTO(updated);
    }

    public CaseExpenseDTO updateExpense(Long expenseId, CaseExpenseDTO dto) {
        log.info("Actualizando gasto ID: {}", expenseId);

        CaseExpense existing = caseExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));

        // ExpenseStatus NO tiene DRAFT, solo PENDING
        // ✅ CORRECTO - ExpenseStatus no tiene DRAFT, solo PENDING
        if (existing.getStatus() != ExpenseStatus.PENDING && existing.getStatus() != ExpenseStatus.REJECTED) {
            throw new RuntimeException("No se puede modificar un gasto que ya ha sido procesado");
        }

        // Actualizar campos modificables (sin cambios)
        if (dto.getCategory() != null) {
            existing.setCategory(dto.getCategory());
        }

        if (dto.getDescription() != null && !dto.getDescription().trim().isEmpty()) {
            existing.setDescription(dto.getDescription());
        }

        if (dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            existing.setAmount(dto.getAmount());
        }

        if (dto.getExpenseDate() != null) {
            existing.setExpenseDate(dto.getExpenseDate().toLocalDate());
        }

        if (dto.getReceiptNumber() != null) {
            existing.setReceiptNumber(dto.getReceiptNumber());
        }

        if (dto.getReceiptPath() != null) {
            existing.setReceiptUrl(dto.getReceiptPath());
        }

        if (dto.getBillableToClient() != null) {
            existing.setBillableToClient(dto.getBillableToClient());
        }

        if (dto.getNotes() != null) {
            existing.setInternalNotes(dto.getNotes());
        }

        CaseExpense updated = caseExpenseRepository.save(existing);
        return convertToCaseExpenseDTO(updated);
    }

    public void deleteExpense(Long expenseId, Long userId) {
        log.info("Eliminando gasto ID: {} por usuario ID: {}", expenseId, userId);

        CaseExpense expense = caseExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));

        // Solo PENDING puede eliminarse (no hay DRAFT en ExpenseStatus)
        if (expense.getStatus() != ExpenseStatus.PENDING && expense.getStatus() != ExpenseStatus.REJECTED) {
            throw new RuntimeException("No se puede eliminar un gasto que ya ha sido procesado");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!expense.getCreatedByUser().getId().equals(userId) && !hasAdminPermissions(user)) {
            throw new RuntimeException("No tiene permisos para eliminar este gasto");
        }

        caseExpenseRepository.delete(expense);
        log.info("Gasto eliminado exitosamente");
    }


    public List<CaseExpenseDTO> getExpensesByCase(Long caseId) {
        log.info("Obteniendo gastos para caso ID: {}", caseId);

        List<CaseExpense> expenses = caseExpenseRepository.findByLegalCaseIdOrderByExpenseDateDesc(caseId);

        return expenses.stream()
                .map(this::convertToCaseExpenseDTO)
                .collect(Collectors.toList());
    }

    public List<CaseExpenseDTO> getPendingExpenses() {
        log.info("Obteniendo gastos pendientes de aprobación");

        List<CaseExpense> pendingExpenses = caseExpenseRepository.findPendingExpenses();

        return pendingExpenses.stream()
                .map(this::convertToCaseExpenseDTO)
                .collect(Collectors.toList());
    }

    public CaseExpenseDTO markAsReimbursed(Long expenseId, Long processedById) {
        log.info("Marcando gasto ID: {} como reembolsado por usuario ID: {}", expenseId, processedById);

        CaseExpense expense = caseExpenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));

        // Verificar que el gasto esté aprobado y no sea facturable al cliente
        if (expense.getStatus() != ExpenseStatus.APPROVED) {
            throw new RuntimeException("Solo se pueden reembolsar gastos aprobados");
        }

        if (expense.getBillableToClient()) {
            throw new RuntimeException("Los gastos facturables al cliente no requieren reembolso");
        }

        if (expense.getReimbursedAt() != null) {
            throw new RuntimeException("Este gasto ya ha sido reembolsado");
        }

        // Marcar como reembolsado
        expense.setReimbursedAt(LocalDateTime.now());

        CaseExpense updated = caseExpenseRepository.save(expense);
        log.info("Gasto marcado como reembolsado exitosamente");

        return convertToCaseExpenseDTO(updated);
    }

    // ====== MÉTODOS HELPER PARA GESTIÓN DE GASTOS ======

    /**
     * Valida los límites presupuestarios para un gasto
     */
    private void validateBudgetLimits(CaseExpense expense) {
        try {
            Optional<FinancialCase> financialCase = financialCaseRepository.findByLegalCaseId(
                    expense.getLegalCase().getId());

            if (financialCase.isPresent() && financialCase.get().getBudgetLimit() != null) {
                BigDecimal budgetLimit = financialCase.get().getBudgetLimit();
                BigDecimal totalExpenses = caseExpenseRepository.getTotalExpensesByCaseId(
                        expense.getLegalCase().getId());

                BigDecimal newTotal = totalExpenses.add(expense.getAmount());

                if (newTotal.compareTo(budgetLimit) > 0) {
                    log.warn("Gasto excede el límite presupuestario. Límite: {}, Total con nuevo gasto: {}",
                            budgetLimit, newTotal);
                    // No lanzar excepción, solo log de advertencia
                }
            }
        } catch (Exception e) {
            log.warn("Error al validar límites presupuestarios: {}", e.getMessage());
        }
    }

    /**
     * Verifica si un usuario tiene permisos de administrador
     */
    private boolean hasAdminPermissions(User user) {
        try {
            // Implementar lógica según tu sistema de roles
            // Por ahora, retornamos false por seguridad
            return false; // Placeholder - implementar según roles del sistema
        } catch (Exception e) {
            log.warn("Error al verificar permisos de administrador: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Cuenta el número de clientes activos por un cliente específico
     */
    private Integer countActiveCasesByClient(Long clientId) {
        try {
            // Implementar lógica para contar casos activos por cliente
            return 3; // Placeholder
        } catch (Exception e) {
            log.warn("Error al contar casos activos para cliente {}: {}", clientId, e.getMessage());
            return 0;
        }
    }
    // ====== REPORTES Y ANÁLISIS ======

    public List<CaseProfitabilityDTO> getCaseProfitabilityReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generando reporte de rentabilidad desde {} hasta {}", startDate, endDate);

        try {
            // Usar datos reales cuando estén disponibles, sino simular
            List<CaseProfitabilityDTO> profitabilityReport = new ArrayList<>();

            // Obtener casos activos o todos los casos
            List<LegalCase> cases = legalCaseRepository.findAll();

            for (LegalCase legalCase : cases.stream().limit(15).collect(Collectors.toList())) {
                BigDecimal totalRevenue = calculateCaseRevenue(legalCase.getId(), startDate, endDate);
                BigDecimal totalExpenses = caseExpenseRepository.getTotalExpensesByCaseId(legalCase.getId());
                if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

                BigDecimal profit = totalRevenue.subtract(totalExpenses);
                BigDecimal profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                        profit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                        BigDecimal.ZERO;

                Long totalHours = calculateCaseHours(legalCase.getId(), startDate, endDate);
                Long billableHours = calculateCaseBillableHours(legalCase.getId(), startDate, endDate);

                profitabilityReport.add(CaseProfitabilityDTO.builder()
                        .caseId(legalCase.getId())
                        .caseNumber(legalCase.getCaseNumber())
                        .estimatedValue(legalCase.getEstimatedValue())
                        .totalRevenue(totalRevenue)
                        .totalExpenses(totalExpenses)
                        .profit(profit)
                        .profitMargin(profitMargin)
                        .totalHours(totalHours)
                        .billableHours(billableHours)
                        .profitabilityRating(calculateProfitabilityRating(profitMargin))
                        .build());
            }

            // Ordenar por rentabilidad descendente
            return profitabilityReport.stream()
                    .sorted((a, b) -> b.getProfitMargin().compareTo(a.getProfitMargin()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Error al obtener datos de rentabilidad, generando datos simulados: {}", e.getMessage());
            return generateSimulatedProfitabilityReport();
        }
    }

    public List<ExpenseSummaryDTO> getExpensesSummary(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generando resumen de gastos desde {} hasta {}", startDate, endDate);

        // ✅ CREAR VARIABLES FINALES
        final LocalDateTime finalStartDate = startDate != null ? startDate : LocalDateTime.now().minusMonths(3);
        final LocalDateTime finalEndDate = endDate != null ? endDate : LocalDateTime.now();

        try {
            List<Object[]> expensesSummary = caseExpenseRepository.getExpensesSummaryByCategory(finalStartDate, finalEndDate);

            List<ExpenseSummaryDTO> summary = expensesSummary.stream()
                    .map(data -> {
                        ExpenseCategory category = (ExpenseCategory) data[0];
                        Long count = (Long) data[1];
                        BigDecimal totalAmount = (BigDecimal) data[2];

                        return ExpenseSummaryDTO.builder()
                                .category(category)
                                .categoryName(getCategoryDisplayName(category))
                                .totalAmount(totalAmount)
                                .expenseCount(count.intValue())
                                .averageAmount(count > 0 ?
                                        totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) :
                                        BigDecimal.ZERO)
                                .period(formatPeriod(finalStartDate, finalEndDate)) // ✅ USAR VARIABLES FINALES
                                .build();
                    })
                    .collect(Collectors.toList());

            // Si no hay datos, generar resumen simulado
            if (summary.isEmpty()) {
                return generateSimulatedExpensesSummary(finalStartDate, finalEndDate); // ✅ USAR VARIABLES FINALES
            }

            return summary;
        } catch (Exception e) {
            log.warn("Error al obtener resumen de gastos, generando datos simulados: {}", e.getMessage());
            return generateSimulatedExpensesSummary(finalStartDate, finalEndDate); // ✅ USAR VARIABLES FINALES
        }
    }

    public TimeAnalyticsDTO getTimeAnalytics(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generando análisis de tiempo para usuario {} desde {} hasta {}", userId, startDate, endDate);

        if (startDate == null) startDate = LocalDateTime.now().minusMonths(1);
        if (endDate == null) endDate = LocalDateTime.now();

        try {
            Long totalMinutes = 0L;
            Long billableMinutes = 0L;

            if (userId != null) {
                // Análisis específico por usuario
                totalMinutes = calculateUserTotalMinutes(userId, startDate, endDate);
                billableMinutes = calculateUserBillableMinutes(userId, startDate, endDate);
            } else {
                // Análisis general de toda la organización
                totalMinutes = timeTrackingSessionRepository.getTotalTrackedTimeByDateRange(startDate, endDate);
                billableMinutes = calculateSystemBillableMinutes(startDate, endDate);
            }

            if (totalMinutes == null) totalMinutes = 0L;
            if (billableMinutes == null) billableMinutes = 0L;

            Long nonBillableMinutes = totalMinutes - billableMinutes;
            Double billablePercentage = totalMinutes > 0 ?
                    (billableMinutes.doubleValue() / totalMinutes.doubleValue() * 100) : 0.0;

            return TimeAnalyticsDTO.builder()
                    .totalMinutesTracked(totalMinutes)
                    .billableMinutes(billableMinutes)
                    .nonBillableMinutes(nonBillableMinutes)
                    .billablePercentage(billablePercentage)
                    .totalRevenue(calculateRevenueFromMinutes(billableMinutes, null))
                    .averageHourlyRate(calculateSystemAverageHourlyRate())
                    .totalSessions(countTimeSessions(userId, startDate, endDate))
                    .averageSessionLength(calculateAverageSessionLength(userId, startDate, endDate))
                    // ✅ CORRECTO
                    .dailyBreakdown(new ArrayList<>()) // Placeholder temporal
                    .caseDistribution(new ArrayList<>()) // Placeholder temporal
                    .build();

        } catch (Exception e) {
            log.warn("Error al generar análisis de tiempo: {}", e.getMessage());
            return generateDefaultTimeAnalytics();
        }
    }

    public CaseFinancialSummaryDTO getCaseFinancialSummary(Long caseId) {
        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Caso no encontrado"));

        try {
            BigDecimal totalRevenue = calculateCaseRevenue(caseId, null, null);
            BigDecimal totalExpenses = caseExpenseRepository.getTotalExpensesByCaseId(caseId);
            BigDecimal billableExpenses = caseExpenseRepository.getBillableExpensesByCaseId(caseId);

            Long totalTrackedMinutes = timeTrackingSessionRepository.getTotalTrackedTimeByCaseId(caseId);
            Long billableMinutes = timeTrackingSessionRepository.getBillableTimeByCaseId(caseId);

            return CaseFinancialSummaryDTO.builder()
                    .caseId(caseId)
                    .caseNumber(legalCase.getCaseNumber())
                    .caseTitle(legalCase.getTitle())
                    .clientName(legalCase.getClient() != null ? legalCase.getClient().getName() : "Cliente no asignado")  // ✅ getName()
                    .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                    .totalExpenses(totalExpenses != null ? totalExpenses : BigDecimal.ZERO)
                    .billableExpenses(billableExpenses != null ? billableExpenses : BigDecimal.ZERO)
                    .totalTrackedHours(totalTrackedMinutes != null ? totalTrackedMinutes / 60.0 : 0.0)
                    .billableHours(billableMinutes != null ? billableMinutes / 60.0 : 0.0)
                    .estimatedValue(legalCase.getEstimatedValue())
                    .build();

        } catch (Exception e) {
            log.warn("Error al calcular resumen financiero de caso: {}", e.getMessage());
            return createDefaultCaseFinancialSummary(legalCase);
        }
    }

    public PendingBillingDTO getPendingBilling() {
        log.info("Obteniendo elementos pendientes de facturación");

        try {
            // Obtener entradas de tiempo listas para facturar
            List<TimeEntry> pendingTimeEntries = timeEntryRepository.findReadyToBill();

            // Obtener gastos listos para facturar
            List<CaseExpense> pendingExpenses = caseExpenseRepository.findPendingExpenses()
                    .stream()
                    .filter(expense -> expense.getStatus() == ExpenseStatus.APPROVED &&
                            expense.getBillableToClient() &&
                            expense.getBilledAt() == null)
                    .collect(Collectors.toList());

            // Calcular totales
            BigDecimal pendingTimeRevenue = pendingTimeEntries.stream()
                    .map(TimeEntry::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal pendingExpenseAmount = pendingExpenses.stream()
                    .map(CaseExpense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return PendingBillingDTO.builder()
                    .pendingTimeEntries(pendingTimeEntries.size())
                    .pendingExpenses(pendingExpenses.size())
                    .totalPendingRevenue(pendingTimeRevenue.add(pendingExpenseAmount))
                    .pendingTimeRevenue(pendingTimeRevenue)
                    .pendingExpenseAmount(pendingExpenseAmount)
                    .build();

        } catch (Exception e) {
            log.warn("Error al obtener elementos pendientes de facturación: {}", e.getMessage());
            return createDefaultPendingBilling();
        }
    }

    // ====== MÉTODOS HELPER PARA REPORTES ======

    private BigDecimal calculateCaseRevenue(Long caseId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            if (startDate != null && endDate != null) {
                // Si no existe findByCaseIdAndDateRange, usar findByLegalCaseId y filtrar
                List<TimeEntry> allEntries = timeEntryRepository.findByLegalCaseId(caseId);
                return allEntries.stream()
                        .filter(entry -> entry.getBillable() &&
                                entry.getWorkDate().isAfter(startDate.toLocalDate().minusDays(1)) &&
                                entry.getWorkDate().isBefore(endDate.toLocalDate().plusDays(1)) &&
                                entry.getTotalAmount() != null)
                        .map(TimeEntry::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                // Usar método que probablemente existe
                List<TimeEntry> entries = timeEntryRepository.findByLegalCaseId(caseId);
                return entries.stream()
                        .filter(entry -> entry.getBillable() && entry.getTotalAmount() != null)
                        .map(TimeEntry::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        } catch (Exception e) {
            log.warn("Error al calcular ingresos del caso {}: {}", caseId, e.getMessage());
            return BigDecimal.valueOf(Math.random() * 50000 + 10000); // Simulado
        }
    }

    private Long calculateCaseHours(Long caseId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            if (startDate != null && endDate != null) {
                // ✅ CORRECTO - usar método que existe
                List<TimeEntry> allEntries = timeEntryRepository.findByLegalCaseId(caseId);
                List<TimeEntry> entries = allEntries.stream()
                        .filter(entry -> entry.getWorkDate().isAfter(startDate.toLocalDate().minusDays(1)) &&
                                entry.getWorkDate().isBefore(endDate.toLocalDate().plusDays(1)))
                        .collect(Collectors.toList());
                return entries.stream()
                        .map(entry -> entry.getDuration() != null ? entry.getDuration().longValue() : 0L)
                        .reduce(0L, Long::sum);
            } else {
                Optional<BigDecimal> hours = timeEntryRepository.getTotalBillableHoursByCase(caseId);
                return hours.orElse(BigDecimal.ZERO).longValue();
            }
        } catch (Exception e) {
            log.warn("Error al calcular horas del caso {}: {}", caseId, e.getMessage());
            return (long) (Math.random() * 200 + 50); // Simulado
        }
    }

    private Long calculateCaseBillableHours(Long caseId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            if (startDate != null && endDate != null) {
                List<TimeEntry> allEntries = timeEntryRepository.findByLegalCaseId(caseId);
                final LocalDate startDateFinal = startDate.toLocalDate();
                final LocalDate endDateFinal = endDate.toLocalDate();
                List<TimeEntry> entries = allEntries.stream()
                        .filter(entry -> entry.getWorkDate().isAfter(startDateFinal.minusDays(1)) &&
                                entry.getWorkDate().isBefore(endDateFinal.plusDays(1)))
                        .collect(Collectors.toList());
                return entries.stream()
                        .filter(TimeEntry::getBillable)
                        .map(entry -> entry.getDuration() != null ? entry.getDuration().longValue() : 0L)
                        .reduce(0L, Long::sum);
            } else {
                Optional<BigDecimal> hours = timeEntryRepository.getTotalBillableHoursByCase(caseId);
                return hours.orElse(BigDecimal.ZERO).longValue();
            }
        } catch (Exception e) {
            log.warn("Error al calcular horas facturables del caso {}: {}", caseId, e.getMessage());
            return (long) (Math.random() * 150 + 40); // Simulado
        }
    }

    private String calculateProfitabilityRating(BigDecimal profitMargin) {
        if (profitMargin.compareTo(BigDecimal.valueOf(30)) >= 0) return "Excelente";
        if (profitMargin.compareTo(BigDecimal.valueOf(20)) >= 0) return "Buena";
        if (profitMargin.compareTo(BigDecimal.valueOf(10)) >= 0) return "Regular";
        if (profitMargin.compareTo(BigDecimal.ZERO) >= 0) return "Baja";
        return "Pérdida";
    }

    private String getCategoryDisplayName(ExpenseCategory category) {
        if (category == null) return "No definida";

        // USAR los valores reales de tu enum ExpenseCategory
        return switch (category) {
            case TRAVEL -> "Viajes";
            case DOCUMENTS -> "Documentos";
            case COURT_FEES -> "Tasas Judiciales";
            case EXPERT_WITNESS -> "Peritos";
            case RESEARCH -> "Investigación";
            case COMMUNICATIONS -> "Comunicaciones";  // ✅ EXISTE
            case OFFICE_SUPPLIES -> "Materiales";
            case EXTERNAL_COUNSEL -> "Abogado Externo";
            case OTHER -> "Otros";
            default -> category.toString();

        };
    }


    private String formatPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " - " +
                endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ====== MÉTODOS DE DATOS SIMULADOS ======

    private List<CaseProfitabilityDTO> generateSimulatedProfitabilityReport() {
        List<CaseProfitabilityDTO> report = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            BigDecimal revenue = BigDecimal.valueOf(30000 + (i * 15000));
            BigDecimal expenses = BigDecimal.valueOf(3000 + (i * 2000));
            BigDecimal profit = revenue.subtract(expenses);
            BigDecimal margin = revenue.compareTo(BigDecimal.ZERO) > 0 ?
                    profit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                    BigDecimal.ZERO;

            report.add(CaseProfitabilityDTO.builder()
                    .caseId((long) i)
                    .caseNumber("2024-00" + String.format("%02d", i))
                    .estimatedValue(revenue.add(BigDecimal.valueOf(10000)))
                    .totalRevenue(revenue)
                    .totalExpenses(expenses)
                    .profit(profit)
                    .profitMargin(margin)
                    .totalHours((long) (80 + (i * 20)))
                    .billableHours((long) (70 + (i * 18)))
                    .profitabilityRating(calculateProfitabilityRating(margin))
                    .build());
        }

        return report;
    }

    private List<ExpenseSummaryDTO> generateSimulatedExpensesSummary(LocalDateTime startDate, LocalDateTime endDate) {
        List<ExpenseSummaryDTO> summary = new ArrayList<>();

        // USAR solo las categorías que existen en tu enum
        ExpenseCategory[] existingCategories = {
                ExpenseCategory.TRAVEL,
                ExpenseCategory.DOCUMENTS,
                ExpenseCategory.COURT_FEES,
                ExpenseCategory.EXPERT_WITNESS,
                ExpenseCategory.RESEARCH,
                ExpenseCategory.COMMUNICATIONS
        };

        for (int i = 0; i < existingCategories.length; i++) {
            final ExpenseCategory category = existingCategories[i]; // ✅ FINAL
            final int index = i;
            BigDecimal amount = BigDecimal.valueOf(1000 + (i * 2500));
            Integer count = 5 + (i * 3);

            summary.add(ExpenseSummaryDTO.builder()
                    .category(category)
                    .categoryName(getCategoryDisplayName(category))
                    .totalAmount(amount)
                    .expenseCount(count)
                    .averageAmount(amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP))
                    .period(formatPeriod(startDate, endDate))
                    .build());
        }

        return summary;
    }

    private TimeAnalyticsDTO generateDefaultTimeAnalytics() {
        return TimeAnalyticsDTO.builder()
                .totalMinutesTracked(7200L) // 120 horas
                .billableMinutes(5400L)     // 90 horas
                .nonBillableMinutes(1800L)  // 30 horas
                .billablePercentage(75.0)
                .totalRevenue(BigDecimal.valueOf(54000))
                .averageHourlyRate(BigDecimal.valueOf(600))
                .totalSessions(45)
                .averageSessionLength(160.0) // 2.67 horas
                .dailyBreakdown(new ArrayList<>())
                .caseDistribution(new ArrayList<>())
                .build();
    }
    // ====== MÉTODOS PRIVADOS DE CÁLCULO ======

    /**
     * Calcula los ingresos totales del sistema
     */
    private BigDecimal calculateTotalRevenue() {
        try {
            Optional<BigDecimal> revenue = timeEntryRepository.getTotalRevenueByDateRange(
                    LocalDate.now().minusYears(1), LocalDate.now());
            return revenue.orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            log.warn("Error al calcular ingresos totales: {}", e.getMessage());
            return BigDecimal.valueOf(500000); // Valor simulado
        }
    }

    /**
     * Calcula los ingresos mensuales en un período específico
     */
    private BigDecimal calculateMonthlyRevenue(LocalDateTime start, LocalDateTime end) {
        try {
            Optional<BigDecimal> revenue = timeEntryRepository.getTotalRevenueByDateRange(
                    start.toLocalDate(), end.toLocalDate());
            return revenue.orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            log.warn("Error al calcular ingresos mensuales: {}", e.getMessage());
            return BigDecimal.valueOf(Math.random() * 50000 + 20000); // Valor simulado
        }
    }

    /**
     * Calcula el crecimiento de ingresos comparando dos períodos
     */
    private BigDecimal calculateRevenueGrowth(LocalDateTime currentStart, LocalDateTime currentEnd,
                                              LocalDateTime previousStart, LocalDateTime previousEnd) {
        BigDecimal currentRevenue = calculateMonthlyRevenue(currentStart, currentEnd);
        BigDecimal previousRevenue = calculateMonthlyRevenue(previousStart, previousEnd);

        if (previousRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return currentRevenue.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }

        return currentRevenue.subtract(previousRevenue)
                .divide(previousRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calcula los gastos totales en un período
     */
    private BigDecimal calculateTotalExpenses(LocalDateTime start, LocalDateTime end) {
        try {
            BigDecimal expenses = caseExpenseRepository.getTotalExpensesByDateRange(start, end);
            return expenses != null ? expenses : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Error al calcular gastos totales: {}", e.getMessage());
            return BigDecimal.valueOf(Math.random() * 15000 + 5000); // Valor simulado
        }
    }

    /**
     * Calcula el margen de beneficio para un período
     */
    private BigDecimal calculateProfitMargin(LocalDateTime start, LocalDateTime end) {
        BigDecimal revenue = calculateMonthlyRevenue(start, end);
        BigDecimal expenses = calculateTotalExpenses(start, end);

        if (revenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return revenue.subtract(expenses)
                .divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calcula las horas totales rastreadas en el sistema
     */
    private Long calculateTotalTrackedHours() {
        try {
            Long totalMinutes = timeTrackingSessionRepository.getTotalTrackedTimeByDateRange(
                    LocalDateTime.now().minusYears(1), LocalDateTime.now());
            return totalMinutes != null ? totalMinutes / 60 : 0L;
        } catch (Exception e) {
            log.warn("Error al calcular horas totales: {}", e.getMessage());
            return (long) (Math.random() * 5000 + 2000); // Valor simulado
        }
    }

    /**
     * Calcula las horas facturables en un período
     */
    private Long calculateBillableHours(LocalDateTime start, LocalDateTime end) {
        try {
            Optional<BigDecimal> totalHours = timeEntryRepository.getTotalHoursByDateRange(
                    start.toLocalDate(), end.toLocalDate());
            return totalHours.orElse(BigDecimal.ZERO).longValue();
        } catch (Exception e) {
            log.warn("Error al calcular horas facturables: {}", e.getMessage());
            return (long) (Math.random() * 800 + 300); // Valor simulado
        }
    }

    /**
     * Calcula la tasa de utilización del sistema
     */
    private Double calculateUtilizationRate(LocalDateTime start, LocalDateTime end) {
        try {
            Long totalMinutes = timeTrackingSessionRepository.getTotalTrackedTimeByDateRange(start, end);
            Long billableMinutes = calculateBillableHours(start, end) * 60;

            if (totalMinutes == null || totalMinutes == 0) {
                return 0.0;
            }

            return Math.min(100.0, (billableMinutes.doubleValue() / totalMinutes.doubleValue() * 100));
        } catch (Exception e) {
            log.warn("Error al calcular tasa de utilización: {}", e.getMessage());
            return 75.0 + (Math.random() * 20); // Valor simulado entre 75-95%
        }
    }

    /**
     * Cuenta las sesiones de tiempo activas
     */
    private Integer countActiveTimeSessions() {
        try {
            Long activeSessions = timeTrackingSessionRepository.countActiveSessions();
            return activeSessions != null ? activeSessions.intValue() : 0;
        } catch (Exception e) {
            log.warn("Error al contar sesiones activas: {}", e.getMessage());
            return (int) (Math.random() * 15 + 5); // Valor simulado
        }
    }

    /**
     * Cuenta los casos activos en el sistema
     */
    private Integer countActiveCases() {
        try {
            List<CaseStatus> activeStatuses = Arrays.asList(CaseStatus.OPEN, CaseStatus.IN_PROGRESS);
            Long activeCases = legalCaseRepository.countByStatusIn(activeStatuses);
            return activeCases != null ? activeCases.intValue() : 0;
        } catch (Exception e) {
            log.warn("Error al contar casos activos: {}", e.getMessage());
            return (int) (Math.random() * 50 + 20); // Valor simulado
        }
    }

    /**
     * Cuenta los casos que requieren facturación
     */
    private Integer countCasesRequiringBilling() {
        try {
            // Si LegalCaseRepository no tiene findCasesRequiringBilling(), simular
            List<TimeEntry> pendingEntries = timeEntryRepository.findByStatus(TimeEntryStatus.APPROVED);
            return (int) pendingEntries.stream()
                    .map(entry -> entry.getLegalCase().getId())
                    .distinct()
                    .count();
        } catch (Exception e) {
            log.warn("Error al contar casos que requieren facturación: {}", e.getMessage());
            return 12; // Valor simulado
        }
    }

    /**
     * Cuenta las facturas vencidas
     */
    private Integer countOverdueInvoices() {
        try {
            // Implementar lógica de facturas vencidas cuando esté disponible
            return 0; // Placeholder - implementar según sistema de facturación
        } catch (Exception e) {
            log.warn("Error al contar facturas vencidas: {}", e.getMessage());
            return (int) (Math.random() * 5); // Valor simulado
        }
    }

    /**
     * Cuenta los abogados activos en el sistema
     */
    private Integer countActiveLawyers() {
        try {
            // Si UserRepository no tiene countActiveLawyers(), usar findAll y filtrar
            List<User> allUsers = userRepository.findAll();
            return (int) allUsers.stream()
                    .filter(user -> user.getActive() &&
                            (user.getRole() == UserRole.LAWYER || user.getRole() == UserRole.MANAGING_PARTNER))
                    .count();
        } catch (Exception e) {
            log.warn("Error al contar abogados activos: {}", e.getMessage());
            return 15; // Valor simulado
        }
    }


    /**
     * Cuenta los abogados que están rastreando tiempo actualmente
     */
    private Integer countLawyersCurrentlyTracking() {
        try {
            // Si UserRepository no tiene findLawyersCurrentlyTracking(), simular
            List<TimeTrackingSession> activeSessions = timeTrackingSessionRepository.findAllActiveSessions();
            return (int) activeSessions.stream()
                    .map(session -> session.getLawyer().getId())
                    .distinct()
                    .count();
        } catch (Exception e) {
            log.warn("Error al contar abogados rastreando tiempo: {}", e.getMessage());
            return 8; // Valor simulado
        }
    }


    /**
     * Cuenta las entradas de tiempo pendientes de aprobación
     */
    private Integer countPendingTimeEntries() {
        try {
            Long pendingEntries = timeEntryRepository.countByCriteria(
                    null, null, TimeEntryStatus.SUBMITTED, null, null, null);
            return pendingEntries != null ? pendingEntries.intValue() : 0;
        } catch (Exception e) {
            log.warn("Error al contar entradas de tiempo pendientes: {}", e.getMessage());
            return (int) (Math.random() * 20 + 5); // Valor simulado
        }
    }

    /**
     * Cuenta los gastos pendientes de aprobación
     */
    private Integer countPendingExpenses() {
        try {
            Long pendingExpenses = caseExpenseRepository.countPendingExpenses();
            return pendingExpenses != null ? pendingExpenses.intValue() : 0;
        } catch (Exception e) {
            log.warn("Error al contar gastos pendientes: {}", e.getMessage());
            return (int) (Math.random() * 12 + 3); // Valor simulado
        }
    }

    /**
     * Calcula el monto total de reembolsos pendientes
     */
    private BigDecimal calculatePendingReimbursements() {
        try {
            BigDecimal pendingAmount = caseExpenseRepository.getTotalPendingReimbursement();
            return pendingAmount != null ? pendingAmount : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Error al calcular reembolsos pendientes: {}", e.getMessage());
            return BigDecimal.valueOf(Math.random() * 8000 + 2000); // Valor simulado
        }
    }

    private Integer countActiveCasesInPeriod(LocalDateTime start, LocalDateTime end) {
        try {
            // Si LegalCaseRepository no tiene countActiveCasesByMonth(), usar findAll
            List<LegalCase> allCases = legalCaseRepository.findAll();
            return (int) allCases.stream()
                    .filter(legalCase -> legalCase.getStatus() == CaseStatus.OPEN ||
                            legalCase.getStatus() == CaseStatus.IN_PROGRESS)
                    .count();
        } catch (Exception e) {
            log.warn("Error al contar casos activos en período: {}", e.getMessage());
            return 25; // Valor simulado
        }
    }


    /**
     * Genera un resumen de facturación para un período
     */
    private List<BillingSummaryDTO> getBillingSummary(LocalDateTime start, LocalDateTime end) {
        List<BillingSummaryDTO> summary = new ArrayList<>();

        try {
            BigDecimal timeRevenue = calculateMonthlyRevenue(start, end);
            BigDecimal expenseAmount = calculateTotalExpenses(start, end);
            BigDecimal totalAmount = timeRevenue.add(expenseAmount);

            // Resumen de tiempo facturado
            summary.add(BillingSummaryDTO.builder()
                    .category("Tiempo Legal")
                    .amount(timeRevenue)
                    .count(countPendingTimeEntries())
                    .period(start.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .percentage(totalAmount.compareTo(BigDecimal.ZERO) > 0 ?
                            timeRevenue.divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                            BigDecimal.ZERO)
                    .status("Facturado")
                    .build());

            // Resumen de gastos facturados
            summary.add(BillingSummaryDTO.builder()
                    .category("Gastos")
                    .amount(expenseAmount)
                    .count(countPendingExpenses())
                    .period(start.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .percentage(totalAmount.compareTo(BigDecimal.ZERO) > 0 ?
                            expenseAmount.divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                            BigDecimal.ZERO)
                    .status("Facturado")
                    .build());

        } catch (Exception e) {
            log.warn("Error al generar resumen de facturación: {}", e.getMessage());

            // Datos simulados en caso de error
            summary.add(BillingSummaryDTO.builder()
                    .category("Tiempo Legal")
                    .amount(BigDecimal.valueOf(35000))
                    .count(45)
                    .period(start.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .percentage(BigDecimal.valueOf(75))
                    .status("Facturado")
                    .build());

            summary.add(BillingSummaryDTO.builder()
                    .category("Gastos")
                    .amount(BigDecimal.valueOf(12000))
                    .count(18)
                    .period(start.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                    .percentage(BigDecimal.valueOf(25))
                    .status("Facturado")
                    .build());
        }

        return summary;
    }

    // ====== MÉTODOS HELPER PARA ANÁLISIS DE TIEMPO ======

    /**
     * Calcula los minutos totales trabajados por un usuario
     */
    private Long calculateUserTotalMinutes(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            return timeTrackingSessionRepository.getTrackedTimeByUserIdAndDateRange(
                    userId, startDate, endDate);
        } catch (Exception e) {
            log.warn("Error al calcular minutos totales para usuario {}: {}", userId, e.getMessage());
            return (long) (Math.random() * 6000 + 2000); // Valor simulado
        }
    }

    /**
     * Calcula los minutos facturables trabajados por un usuario
     */
    private Long calculateUserBillableMinutes(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<TimeEntry> entries = timeEntryRepository.findByLawyerIdAndWorkDateBetweenOrderByWorkDateDesc(
                    userId, startDate.toLocalDate(), endDate.toLocalDate());

            return entries.stream()
                    .filter(TimeEntry::getBillable)
                    .map(entry -> entry.getDuration() != null ?
                            entry.getDuration().multiply(BigDecimal.valueOf(60)).longValue() : 0L)
                    .reduce(0L, Long::sum);
        } catch (Exception e) {
            log.warn("Error al calcular minutos facturables para usuario {}: {}", userId, e.getMessage());
            return (long) (Math.random() * 4500 + 1500); // Valor simulado
        }
    }

    /**
     * Calcula los minutos facturables del sistema
     */
    private Long calculateSystemBillableMinutes(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Optional<BigDecimal> totalHours = timeEntryRepository.getTotalHoursByDateRange(
                    startDate.toLocalDate(), endDate.toLocalDate());
            return totalHours.orElse(BigDecimal.ZERO).multiply(BigDecimal.valueOf(60)).longValue();
        } catch (Exception e) {
            log.warn("Error al calcular minutos facturables del sistema: {}", e.getMessage());
            return (long) (Math.random() * 15000 + 8000); // Valor simulado
        }
    }

    /**
     * Calcula la tarifa promedio por hora del sistema
     */
    private BigDecimal calculateSystemAverageHourlyRate() {
        try {
            Optional<BigDecimal> avgRate = timeEntryRepository.getAverageHourlyRate();
            return avgRate.orElse(BigDecimal.valueOf(600));
        } catch (Exception e) {
            log.warn("Error al calcular tarifa promedio del sistema: {}", e.getMessage());
            return BigDecimal.valueOf(550 + (Math.random() * 200)); // Valor simulado
        }
    }

    /**
     * Cuenta las sesiones de tiempo en un período
     */
    private Integer countTimeSessions(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            if (userId != null) {
                List<TimeTrackingSession> sessions = timeTrackingSessionRepository
                        .findByUserIdAndDateRange(userId, startDate, endDate);
                return sessions.size();
            } else {
                List<TimeTrackingSession> sessions = timeTrackingSessionRepository
                        .findByDateRange(startDate, endDate);
                return sessions.size();
            }
        } catch (Exception e) {
            log.warn("Error al contar sesiones de tiempo: {}", e.getMessage());
            return (int) (Math.random() * 50 + 20); // Valor simulado
        }
    }

    /**
     * Calcula la duración promedio de sesiones
     */
    private Double calculateAverageSessionLength(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            if (userId != null) {
                Double avgDuration = timeTrackingSessionRepository.getAverageSessionDurationByUserId(userId);
                return avgDuration != null ? avgDuration : 120.0;
            } else {
                // Calcular promedio general del sistema
                return 135.0; // Placeholder
            }
        } catch (Exception e) {
            log.warn("Error al calcular duración promedio de sesiones: {}", e.getMessage());
            return 120.0 + (Math.random() * 60); // Valor simulado
        }
    }

    /**
     * Calcula ingresos a partir de minutos trabajados
     */
    private BigDecimal calculateRevenueFromMinutes(Long minutes, Long caseId) {
        if (minutes == null || minutes == 0) return BigDecimal.ZERO;

        BigDecimal hourlyRate = caseId != null ? getHourlyRateForCase(caseId) : calculateSystemAverageHourlyRate();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return hours.multiply(hourlyRate);
    }

    // ====== MÉTODOS HELPER PARA DATOS SIMULADOS ======

    private List<DailyTimeDTO> generateDailyBreakdown(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return new ArrayList<>(); // Placeholder
    }

    private List<Object> generateCaseDistribution(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return new ArrayList<>(); // Placeholder
    }

    private CaseFinancialSummaryDTO createDefaultCaseFinancialSummary(LegalCase legalCase) {
        return CaseFinancialSummaryDTO.builder()
                .caseId(legalCase.getId())
                .caseNumber(legalCase.getCaseNumber())
                .caseTitle(legalCase.getTitle())
                .clientName(legalCase.getClient() != null ? legalCase.getClient().getName() : "Sin cliente")  // ✅ getName()
                .totalRevenue(BigDecimal.ZERO)
                .totalExpenses(BigDecimal.ZERO)
                .billableExpenses(BigDecimal.ZERO)
                .totalTrackedHours(0.0)
                .billableHours(0.0)
                .estimatedValue(legalCase.getEstimatedValue())
                .build();
    }

    private PendingBillingDTO createDefaultPendingBilling() {
        return PendingBillingDTO.builder()
                .pendingTimeEntries(0)
                .pendingExpenses(0)
                .totalPendingRevenue(BigDecimal.ZERO)
                .pendingTimeRevenue(BigDecimal.ZERO)
                .pendingExpenseAmount(BigDecimal.ZERO)
                .build();
    }
    // ====== MÉTODOS DE CONVERSIÓN A DTOs ======

    /**
     * Convierte una entidad FinancialCase a DTO
     */
    private FinancialCaseDTO convertToFinancialCaseDTO(FinancialCase entity) {
        if (entity == null) {
            return null;
        }

        try {
            BigDecimal totalBilled = calculateCaseRevenue(entity.getLegalCase().getId(), null, null);
            BigDecimal totalExpenses = caseExpenseRepository.getTotalExpensesByCaseId(entity.getLegalCase().getId());
            Integer totalTimeMinutes = calculateCaseTotalMinutes(entity.getLegalCase().getId());

            return FinancialCaseDTO.builder()
                    .id(entity.getId())
                    .legalCaseId(entity.getLegalCase().getId())
                    .caseNumber(entity.getLegalCase().getCaseNumber())
                    .caseTitle(entity.getLegalCase().getTitle())
                    .billingType(entity.getBillingType())
                    .hourlyRate(entity.getHourlyRate())
                    .fixedFee(entity.getFixedFee())
                    .contingencyPercentage(entity.getContingencyPercentage())
                    .retainerAmount(entity.getRetainerAmount())
                    .budgetLimit(entity.getBudgetLimit())
                    .billingNotes(entity.getBillingNotes())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .clientName(entity.getLegalCase().getClient() != null ?
                            entity.getLegalCase().getClient().getName() : "Sin cliente asignado")  // ✅ getName()
                    .totalBilled(totalBilled != null ? totalBilled : BigDecimal.ZERO)
                    .totalExpenses(totalExpenses != null ? totalExpenses : BigDecimal.ZERO)
                    .totalTimeMinutes(totalTimeMinutes != null ? totalTimeMinutes : 0)
                    .build();

        } catch (Exception e) {
            log.warn("Error al convertir FinancialCase a DTO: {}", e.getMessage());

            return FinancialCaseDTO.builder()
                    .id(entity.getId())
                    .legalCaseId(entity.getLegalCase().getId())
                    .caseNumber(entity.getLegalCase().getCaseNumber())
                    .caseTitle(entity.getLegalCase().getTitle())
                    .billingType(entity.getBillingType())
                    .hourlyRate(entity.getHourlyRate())
                    .fixedFee(entity.getFixedFee())
                    .contingencyPercentage(entity.getContingencyPercentage())
                    .retainerAmount(entity.getRetainerAmount())
                    .budgetLimit(entity.getBudgetLimit())
                    .billingNotes(entity.getBillingNotes())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .clientName("Cliente no disponible")
                    .totalBilled(BigDecimal.ZERO)
                    .totalExpenses(BigDecimal.ZERO)
                    .totalTimeMinutes(0)
                    .build();
        }
    }



    /**
     * Convierte una entidad TimeEntry a DTO
     */
    private TimeEntryDTO convertToTimeEntryDTO(TimeEntry entity) {
        if (entity == null) {
            return null;
        }

        try {
            Integer totalMinutes = entity.getDuration() != null ?
                    entity.getDuration().multiply(BigDecimal.valueOf(60)).intValue() : 0;
            Integer billableMinutes = entity.getBillable() ? totalMinutes : 0;

            return TimeEntryDTO.builder()
                    .id(entity.getId())
                    .legalCaseId(entity.getLegalCase().getId())
                    .caseNumber(entity.getLegalCase().getCaseNumber())
                    .caseTitle(entity.getLegalCase().getTitle())
                    .userId(entity.getLawyer().getId()) // ✅ USAR getLawyer()
                    .lawyerName(formatLawyerName(entity.getLawyer())) // ✅ USAR getLawyer()
                    .description(entity.getDescription())
                    .startTime(entity.getWorkDate() != null ? entity.getWorkDate().atStartOfDay() : null)
                    .endTime(entity.getWorkDate() != null && entity.getDuration() != null ?
                            entity.getWorkDate().atStartOfDay().plusMinutes(totalMinutes.longValue()) : null)
                    .totalMinutes(totalMinutes)
                    .billableMinutes(billableMinutes)
                    .hourlyRate(entity.getHourlyRate())
                    .billableAmount(entity.getTotalAmount())
                    .status(entity.getStatus())
                    .rejectionReason(null)
                    .reviewedById(null)
                    .reviewedByName(null)
                    .reviewedAt(null)
                    .billedAt(null)
                    .billingNotes(null)
                    .createdAt(entity.getCreatedAt())
                    .isBillable(entity.getBillable())
                    .build();

        } catch (Exception e) {
            log.warn("Error al convertir TimeEntry a DTO: {}", e.getMessage());

            return TimeEntryDTO.builder()
                    .id(entity.getId())
                    .legalCaseId(entity.getLegalCase() != null ? entity.getLegalCase().getId() : null)
                    .caseNumber("N/A")
                    .caseTitle("N/A")
                    .userId(entity.getLawyer() != null ? entity.getLawyer().getId() : null) // ✅ USAR getLawyer()
                    .lawyerName("N/A")
                    .description(entity.getDescription())
                    .totalMinutes(0)
                    .billableMinutes(0)
                    .hourlyRate(entity.getHourlyRate())
                    .billableAmount(entity.getTotalAmount())
                    .status(entity.getStatus())
                    .createdAt(entity.getCreatedAt())
                    .isBillable(entity.getBillable())
                    .build();
        }
    }

    /**
     * Convierte una entidad CaseExpense a DTO
     */
    private CaseExpenseDTO convertToCaseExpenseDTO(CaseExpense entity) {
        if (entity == null) {
            return null;
        }

        try {
            return CaseExpenseDTO.builder()
                    .id(entity.getId())
                    .legalCaseId(entity.getLegalCase().getId())
                    .caseNumber(entity.getLegalCase().getCaseNumber())
                    .caseTitle(entity.getLegalCase().getTitle())
                    .category(entity.getCategory())
                    .description(entity.getDescription())
                    .amount(entity.getAmount())
                    // CORRECCIÓN: CONVERTIR LocalDate a LocalDateTime
                    .expenseDate(entity.getExpenseDate() != null ?
                            entity.getExpenseDate().atStartOfDay() : null)
                    .receiptNumber(entity.getReceiptNumber())
                    .receiptPath(entity.getReceiptUrl()) // USAR receiptUrl
                    .billableToClient(entity.getBillableToClient())
                    .status(entity.getStatus())
                    // USAR createdByUser:
                    .submittedById(entity.getCreatedByUser().getId())
                    .submittedByName(formatUserName(entity.getCreatedByUser()))
                    .submittedAt(entity.getCreatedAt()) // USAR createdAt
                    // USAR approvedByUser:
                    .reviewedById(entity.getApprovedByUser() != null ? entity.getApprovedByUser().getId() : null)
                    .reviewedByName(entity.getApprovedByUser() != null ? formatUserName(entity.getApprovedByUser()) : null)
                    .reviewedAt(entity.getApprovedAt()) // USAR approvedAt
                    .reimbursedAt(entity.getReimbursedAt())
                    .billedAt(entity.getBilledAt())
                    .notes(entity.getInternalNotes()) // USAR internalNotes
                    .rejectionReason(entity.getRejectionReason())
                    .build();

        } catch (Exception e) {
            log.warn("Error al convertir CaseExpense a DTO: {}", e.getMessage());

            return CaseExpenseDTO.builder()
                    .id(entity.getId())
                    .legalCaseId(entity.getLegalCase() != null ? entity.getLegalCase().getId() : null)
                    .caseNumber("N/A")
                    .caseTitle("N/A")
                    .category(entity.getCategory())
                    .description(entity.getDescription())
                    .amount(entity.getAmount())
                    .expenseDate(entity.getExpenseDate() != null ?
                            entity.getExpenseDate().atStartOfDay() : null)
                    .billableToClient(entity.getBillableToClient())
                    .status(entity.getStatus())
                    .submittedById(entity.getCreatedByUser() != null ? entity.getCreatedByUser().getId() : null)
                    .submittedByName("Usuario no disponible")
                    .submittedAt(entity.getCreatedAt())
                    .notes(entity.getInternalNotes())
                    .build();
        }
    }

    /**
     * Convierte una lista de entidades FinancialCase a DTOs
     */
    private List<FinancialCaseDTO> convertToFinancialCaseDTOList(List<FinancialCase> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        return entities.stream()
                .map(this::convertToFinancialCaseDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una lista de entidades TimeEntry a DTOs
     */
    private List<TimeEntryDTO> convertToTimeEntryDTOList(List<TimeEntry> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        return entities.stream()
                .map(this::convertToTimeEntryDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una lista de entidades CaseExpense a DTOs
     */
    private List<CaseExpenseDTO> convertToCaseExpenseDTOList(List<CaseExpense> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        return entities.stream()
                .map(this::convertToCaseExpenseDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ====== MÉTODOS HELPER PARA CONVERSIÓN ======

    /**
     * Formatea el nombre completo de un abogado
     */
    private String formatLawyerName(User lawyer) {
        if (lawyer == null) {
            return "Abogado no asignado";
        }

        try {
            String firstName = lawyer.getFirstName() != null ? lawyer.getFirstName() : "";
            String lastName = lawyer.getLastName() != null ? lawyer.getLastName() : "";

            String fullName = (firstName + " " + lastName).trim();
            return fullName.isEmpty() ? "Nombre no disponible" : fullName;
        } catch (Exception e) {
            log.warn("Error al formatear nombre de abogado: {}", e.getMessage());
            return "Nombre no disponible";
        }
    }

    /**
     * Formatea el nombre completo de un usuario
     */
    private String formatUserName(User user) {
        if (user == null) {
            return "Usuario no asignado";
        }

        try {
            String firstName = user.getFirstName() != null ? user.getFirstName() : "";
            String lastName = user.getLastName() != null ? user.getLastName() : "";

            String fullName = (firstName + " " + lastName).trim();
            return fullName.isEmpty() ? "Nombre no disponible" : fullName;
        } catch (Exception e) {
            log.warn("Error al formatear nombre de usuario: {}", e.getMessage());
            return "Nombre no disponible";
        }
    }

    /**
     * Calcula los minutos totales trabajados en un caso
     */
    private Integer calculateCaseTotalMinutes(Long caseId) {
        try {
            Long totalMinutes = timeTrackingSessionRepository.getTotalTrackedTimeByCaseId(caseId);
            return totalMinutes != null ? totalMinutes.intValue() : 0;
        } catch (Exception e) {
            log.warn("Error al calcular minutos totales para caso {}: {}", caseId, e.getMessage());
            return 0;
        }
    }

    /**
     * Formatea un período de tiempo para mostrar
     */
    private String formatPeriodDisplay(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "Período no especificado";
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return start.format(formatter) + " - " + end.format(formatter);
        } catch (Exception e) {
            log.warn("Error al formatear período: {}", e.getMessage());
            return "Período inválido";
        }
    }

    /**
     * Convierte minutos a formato de horas legible
     */
    private String formatMinutesToHours(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return "0h 0m";
        }

        try {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            return String.format("%dh %dm", hours, remainingMinutes);
        } catch (Exception e) {
            log.warn("Error al formatear minutos a horas: {}", e.getMessage());
            return "0h 0m";
        }
    }

    /**
     * Valida y formatea un monto monetario
     */
    private BigDecimal validateAndFormatAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        try {
            return amount.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Error al validar monto: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Obtiene el nombre del estado de una entrada de tiempo de forma legible
     */
    private String getTimeEntryStatusDisplayName(TimeEntryStatus status) {
        if (status == null) {
            return "Estado desconocido";
        }

        return switch (status) {
            case DRAFT -> "Borrador";
            case SUBMITTED -> "Enviado";
            case APPROVED -> "Aprobado";
            case REJECTED -> "Rechazado";
            case BILLED -> "Facturado";
            default -> status.name();
        };
    }

    /**
     * Obtiene el nombre del estado de un gasto de forma legible
     */
    private String getExpenseStatusDisplayName(ExpenseStatus status) {
        if (status == null) {
            return "Estado desconocido";
        }

        return switch (status) {

            case PENDING -> "Pendiente";
            case APPROVED -> "Aprobado";
            case REJECTED -> "Rechazado";
            case REIMBURSED -> "Reembolsado";
            default -> status.name();
        };
    }

    /**
     * Calcula el porcentaje de un valor sobre un total
     */
    private BigDecimal calculatePercentage(BigDecimal value, BigDecimal total) {
        if (value == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        try {
            return value.divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Error al calcular porcentaje: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Valida que un ID sea válido (no nulo y mayor a 0)
     */
    private boolean isValidId(Long id) {
        return id != null && id > 0;
    }

    /**
     * Obtiene la descripción truncada para mostrar en listas
     */
    private String getTruncatedDescription(String description, int maxLength) {
        if (description == null || description.trim().isEmpty()) {
            return "Sin descripción";
        }

        String trimmed = description.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }

        return trimmed.substring(0, maxLength - 3) + "...";
    }

    /**
     * Convierte un LocalDateTime a String con formato específico
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        try {
            return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            log.warn("Error al formatear fecha: {}", e.getMessage());
            return dateTime.toString();
        }
    }

// ====== FIN DE LA CLASE ======
}