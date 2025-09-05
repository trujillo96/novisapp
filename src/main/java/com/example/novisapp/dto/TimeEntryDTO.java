// ====== ARCHIVO: TimeEntryDTO.java ======
package com.example.novisapp.dto;

import com.example.novisapp.entity.TimeEntryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryDTO {
    private Long id;
    private Long legalCaseId;
    private String caseNumber;
    private String caseTitle;
    private Long userId;
    private String lawyerName;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalMinutes;
    private Integer billableMinutes;
    private BigDecimal hourlyRate;
    private BigDecimal billableAmount;
    private TimeEntryStatus status;
    private String rejectionReason;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime billedAt;
    private String billingNotes;
    private LocalDateTime createdAt;
    private Boolean isBillable;
}