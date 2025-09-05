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
public class TimeTrackingSessionDTO {
    private Long id;
    private Long userId;
    private String userName;
    private Long legalCaseId;
    private String caseNumber;
    private String caseTitle;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalMinutes;
    private Boolean isActive;
    private Boolean isBillable;
    private String notes;
    private Boolean convertedToTimeEntry;
    private LocalDateTime createdAt;
    private Long currentDurationMinutes;
}
