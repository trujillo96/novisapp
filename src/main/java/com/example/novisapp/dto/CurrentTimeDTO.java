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
public class CurrentTimeDTO {
    private Long sessionId;
    private Long currentMinutes;
    private String formattedTime;
    private Boolean isActive;
    private LocalDateTime startTime;
}