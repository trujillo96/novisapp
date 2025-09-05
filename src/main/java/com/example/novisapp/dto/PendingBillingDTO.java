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
public class PendingBillingDTO {
    private Integer pendingTimeEntries;
    private Integer pendingExpenses;
    private BigDecimal totalPendingRevenue;
    private BigDecimal pendingTimeRevenue;
    private BigDecimal pendingExpenseAmount;
}