// ====== ARCHIVO: CaseProfitabilityDTO.java ======
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
public class CaseProfitabilityDTO {
    private Long caseId;
    private String caseNumber;
    private String caseTitle;
    private String clientName;
    private BigDecimal estimatedValue;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal profit;
    private BigDecimal profitMargin;
    private Long totalHours;
    private Long billableHours;
    private Double utilizationRate;
    private String status;
    private String profitabilityRating; // "High", "Medium", "Low", "Loss"
}
