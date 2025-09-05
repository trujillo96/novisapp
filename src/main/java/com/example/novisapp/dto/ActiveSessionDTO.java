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
public class ActiveSessionDTO {
    private Long sessionId;
    private Long userId;
    private String userName;
    private Long caseId;
    private String caseNumber;
    private String caseTitle;
    private String clientName;
    private LocalDateTime startTime;
    private Long currentMinutes;
    private String description;
    private Boolean isBillable;
    private String status;
}