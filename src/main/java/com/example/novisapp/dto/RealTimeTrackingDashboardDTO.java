package com.example.novisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeTrackingDashboardDTO {
    private Integer activeSessionsCount;
    private Long totalActiveMinutes;
    private Long todayTrackedMinutes;
    private Long weekTrackedMinutes;
    private Long monthTrackedMinutes;
    private List<ActiveSessionDTO> activeSessions;
}
