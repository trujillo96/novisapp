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
public class BillingSummaryDTO {
    private String category; // "Time", "Expenses", "Fixed Fees"
    private BigDecimal amount;
    private Integer count;
    private String period; // "2024-01"
    private BigDecimal percentage;
    private String status; // "Billed", "Pending", "Draft"
}