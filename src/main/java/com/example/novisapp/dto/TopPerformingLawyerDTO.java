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
public class TopPerformingLawyerDTO {
    private Long lawyerId;
    private String firstName;
    private String lastName;
    private String fullName;
    private Long totalBillableHours;
    private BigDecimal totalRevenue;
    private BigDecimal averageHourlyRate;
    private Integer activeCases;
    private Double utilizationRate;
    private Integer timeEntriesCount;
    private String specialization;
}
