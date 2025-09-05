package com.example.novisapp.dto;

import com.example.novisapp.entity.BillingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialCaseDTO {
    private Long id;
    private Long legalCaseId;
    private String caseNumber;
    private String caseTitle;
    private BillingType billingType;
    private BigDecimal hourlyRate;
    private BigDecimal fixedFee;
    private BigDecimal contingencyPercentage;
    private BigDecimal retainerAmount;
    private BigDecimal budgetLimit;
    private Boolean billingEnabled;
    private String billingNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String clientName;
    private BigDecimal totalBilled;
    private BigDecimal totalExpenses;
    private Integer totalTimeMinutes;
}