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
public class MonthlyRevenueDTO {
    private String month;
    private Integer year;
    private String monthYear; // "2024-01"
    private BigDecimal revenue;
    private BigDecimal expenses;
    private BigDecimal profit;
    private Long billableHours;
    private Integer activeCases;
    private BigDecimal growthRate;
}
