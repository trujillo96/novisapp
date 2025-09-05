// ====== ARCHIVO: FinancialDashboardDTO.java ======
package com.example.novisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialDashboardDTO {
    // KPIs principales
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal revenueGrowth;
    private BigDecimal totalExpenses;
    private BigDecimal profitMargin;

    // Métricas de tiempo
    private Long totalTrackedHours;
    private Long billableHours;
    private Double utilizationRate;
    private Integer activeTimeSessions;

    // Métricas de casos
    private Integer totalActiveCases;
    private Integer casesRequiringBilling;
    private Integer overdueInvoices;

    // Métricas de equipo
    private Integer activeLawyers;
    private Integer lawyersTracking;

    // Pendientes de aprobación
    private Integer pendingTimeEntries;
    private Integer pendingExpenses;
    private BigDecimal pendingReimbursements;

    // Datos para gráficos
    private List<MonthlyRevenueDTO> revenueHistory;
    private List<TopRevenueClientDTO> topClients;
    private List<TopPerformingLawyerDTO> topLawyers;
    private List<BillingSummaryDTO> billingSummary;
}