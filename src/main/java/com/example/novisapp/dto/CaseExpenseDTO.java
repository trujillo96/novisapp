package com.example.novisapp.dto;

import com.example.novisapp.entity.ExpenseCategory;
import com.example.novisapp.entity.ExpenseStatus;
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
public class CaseExpenseDTO {
    private Long id;
    private Long legalCaseId;
    private String caseNumber;
    private String caseTitle;
    private ExpenseCategory category;
    private String description;
    private BigDecimal amount;
    private LocalDateTime expenseDate;
    private String receiptNumber;
    private String receiptPath;
    private Boolean billableToClient;
    private ExpenseStatus status;
    private Long submittedById;
    private String submittedByName;
    private LocalDateTime submittedAt;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime reimbursedAt;
    private LocalDateTime billedAt;
    private String notes;
    private String rejectionReason;
}