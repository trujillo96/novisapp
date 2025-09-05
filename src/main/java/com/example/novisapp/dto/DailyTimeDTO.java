package com.example.novisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTimeDTO {
    private LocalDate date;
    private String dayOfWeek;
    private Long totalMinutes;
    private Long billableMinutes;
    private Integer sessionCount;
    private Double utilizationRate;
}