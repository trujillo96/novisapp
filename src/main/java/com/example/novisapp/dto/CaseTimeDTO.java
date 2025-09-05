package com.example.novisapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseTimeDTO {
    private Long caseId;
    private String caseNumber;
    private String caseTitle;
    private Long totalMinutes;
    private Long billableMinutes;
    private Double percentage;
}