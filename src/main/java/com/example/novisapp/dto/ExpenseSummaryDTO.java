package com.example.novisapp.dto;

import com.example.novisapp.entity.ExpenseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummaryDTO {
    private ExpenseCategory category;
    private String categoryName;
    private BigDecimal totalAmount;
    private Integer expenseCount;
    private BigDecimal averageAmount;
    private BigDecimal billableAmount;
    private BigDecimal reimbursableAmount;
    private String period;
}
