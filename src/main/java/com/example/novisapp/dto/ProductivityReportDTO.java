package com.example.novisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductivityReportDTO {
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Integer totalLawyers;
    private Integer topPerformers;
}
