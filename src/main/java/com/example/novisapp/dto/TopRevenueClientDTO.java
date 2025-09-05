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
public class TopRevenueClientDTO {
    private Long clientId;
    private String clientName;
    private String companyName;
    private BigDecimal totalRevenue;
    private BigDecimal totalBilled;
    private Long totalBillableHours;
    private Integer activeCases;
    private Integer completedCases;
    private BigDecimal averageCaseValue;
    private String lastActivityDate;
}
