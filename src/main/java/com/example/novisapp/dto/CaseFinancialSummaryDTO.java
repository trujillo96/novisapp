package com.example.novisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseFinancialSummaryDTO {
    private Long caseId;
    private String caseNumber;
    private String caseTitle;
    private String clientName;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal billableExpenses;
    private Double totalTrackedHours;
    private Double billableHours;
    private BigDecimal estimatedValue;
}
