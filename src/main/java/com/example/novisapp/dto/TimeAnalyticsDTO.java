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
public class TimeAnalyticsDTO {
    private Long totalMinutesTracked;
    private Long billableMinutes;
    private Long nonBillableMinutes;
    private Double billablePercentage;
    private BigDecimal totalRevenue;
    private BigDecimal averageHourlyRate;
    private Integer totalSessions;
    private Double averageSessionLength;
    private List<DailyTimeDTO> dailyBreakdown;
    private List<CaseTimeDTO> caseDistribution;
}
