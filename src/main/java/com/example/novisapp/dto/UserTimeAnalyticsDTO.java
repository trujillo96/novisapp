package com.example.novisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTimeAnalyticsDTO {
    private Long userId;
    private Long totalMinutes;
    private Double totalHours;
    private Double averageSessionDuration;
    private String period;
}