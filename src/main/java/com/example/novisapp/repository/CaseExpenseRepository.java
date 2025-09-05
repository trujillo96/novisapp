package com.example.novisapp.repository;

import com.example.novisapp.entity.CaseExpense;
import com.example.novisapp.entity.ExpenseCategory;
import com.example.novisapp.entity.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CaseExpense entity
 * Handles all database operations for case expenses and financial tracking
 */
@Repository
public interface CaseExpenseRepository extends JpaRepository<CaseExpense, Long> {

    // ====== BASIC FINDER METHODS ======

    /**
     * Find all expenses for a specific case
     */
    List<CaseExpense> findByLegalCaseIdOrderByExpenseDateDesc(Long caseId);

    /**
     * Find expenses by case with pagination
     */
    Page<CaseExpense> findByLegalCaseId(Long caseId, Pageable pageable);

    /**
     * Find expenses by status
     */
    List<CaseExpense> findByStatusOrderByExpenseDateDesc(ExpenseStatus status);

    /**
     * Find expenses by category
     */
    List<CaseExpense> findByCategoryOrderByExpenseDateDesc(ExpenseCategory category);

    /**
     * Find expenses by submitter - ✅ CORREGIDO: usar createdByUser
     */
    List<CaseExpense> findByCreatedByUserIdOrderByExpenseDateDesc(Long createdByUserId);

    // ====== FINANCIAL CALCULATIONS ======

    /**
     * Calculate total expenses for a case
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM CaseExpense e WHERE e.legalCase.id = :caseId")
    BigDecimal getTotalExpensesByCaseId(@Param("caseId") Long caseId);

    /**
     * Calculate approved expenses for a case
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM CaseExpense e WHERE e.legalCase.id = :caseId AND e.status = 'APPROVED'")
    BigDecimal getApprovedExpensesByCaseId(@Param("caseId") Long caseId);

    /**
     * Calculate billable expenses for a case
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM CaseExpense e WHERE e.legalCase.id = :caseId AND e.billableToClient = true AND e.status = 'APPROVED'")
    BigDecimal getBillableExpensesByCaseId(@Param("caseId") Long caseId);

    /**
     * Calculate total expenses by category for a case
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM CaseExpense e WHERE e.legalCase.id = :caseId AND e.category = :category AND e.status = 'APPROVED'")
    BigDecimal getExpensesByCaseAndCategory(@Param("caseId") Long caseId, @Param("category") ExpenseCategory category);

    // ====== DATE RANGE QUERIES ======

    /**
     * Find expenses within date range
     */
    @Query("SELECT e FROM CaseExpense e WHERE e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<CaseExpense> findByExpenseDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total expenses for date range
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM CaseExpense e WHERE e.expenseDate BETWEEN :startDate AND :endDate AND e.status = 'APPROVED'")
    BigDecimal getTotalExpensesByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find expenses by case and date range
     */
    @Query("SELECT e FROM CaseExpense e WHERE e.legalCase.id = :caseId AND e.expenseDate BETWEEN :startDate AND :endDate ORDER BY e.expenseDate DESC")
    List<CaseExpense> findByCaseAndDateRange(@Param("caseId") Long caseId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ====== CLIENT-RELATED QUERIES ======

    /**
     * Find expenses by client
     */
    @Query("SELECT e FROM CaseExpense e WHERE e.legalCase.client.id = :clientId ORDER BY e.expenseDate DESC")
    List<CaseExpense> findByClientId(@Param("clientId") Long clientId);

    /**
     * Calculate total expenses for a client
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM CaseExpense e WHERE e.legalCase.client.id = :clientId AND e.status = 'APPROVED'")
    BigDecimal getTotalExpensesByClientId(@Param("clientId") Long clientId);

    /**
     * Calculate billable expenses for a client
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM CaseExpense e WHERE e.legalCase.client.id = :clientId AND e.billableToClient = true AND e.status = 'APPROVED'")
    BigDecimal getBillableExpensesByClientId(@Param("clientId") Long clientId);

    // ====== PENDING APPROVALS ======

    /**
     * Find pending expenses for approval - ✅ CORREGIDO: usar createdAt
     */
    @Query("SELECT e FROM CaseExpense e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<CaseExpense> findPendingExpenses();

    /**
     * Count pending expenses
     */
    @Query("SELECT COUNT(e) FROM CaseExpense e WHERE e.status = 'PENDING'")
    Long countPendingExpenses();

    /**
     * Find pending expenses by amount threshold
     */
    @Query("SELECT e FROM CaseExpense e WHERE e.status = 'PENDING' AND e.amount >= :threshold ORDER BY e.amount DESC")
    List<CaseExpense> findPendingExpensesAboveThreshold(@Param("threshold") BigDecimal threshold);

    // ====== REIMBURSEMENT TRACKING ======

    /**
     * Find reimbursed expenses
     */
    List<CaseExpense> findByStatusAndReimbursedAtIsNotNull(ExpenseStatus status);

    /**
     * Find expenses pending reimbursement
     */
    @Query("SELECT e FROM CaseExpense e WHERE e.status = 'APPROVED' AND e.billableToClient = false AND e.reimbursedAt IS NULL")
    List<CaseExpense> findPendingReimbursement();

    /**
     * Calculate total pending reimbursement
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM CaseExpense e WHERE e.status = 'APPROVED' AND e.billableToClient = false AND e.reimbursedAt IS NULL")
    BigDecimal getTotalPendingReimbursement();

    // ====== REPORTING QUERIES ======

    /**
     * Get expenses summary by category for reporting
     */
    @Query("SELECT e.category, COUNT(e), SUM(e.amount) FROM CaseExpense e WHERE e.status = 'APPROVED' AND e.expenseDate BETWEEN :startDate AND :endDate GROUP BY e.category")
    List<Object[]> getExpensesSummaryByCategory(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get top expense cases by total amount
     */
    @Query("SELECT e.legalCase.id, e.legalCase.caseNumber, SUM(e.amount) as totalExpenses FROM CaseExpense e WHERE e.status = 'APPROVED' GROUP BY e.legalCase.id, e.legalCase.caseNumber ORDER BY totalExpenses DESC")
    List<Object[]> getTopExpenseCases(Pageable pageable);

    /**
     * Get monthly expenses trend - ✅ CORREGIDO: usar FUNCTION
     */
    @Query("SELECT FUNCTION('YEAR', e.expenseDate), FUNCTION('MONTH', e.expenseDate), SUM(e.amount) FROM CaseExpense e WHERE e.status = 'APPROVED' AND e.expenseDate >= :startDate GROUP BY FUNCTION('YEAR', e.expenseDate), FUNCTION('MONTH', e.expenseDate) ORDER BY FUNCTION('YEAR', e.expenseDate), FUNCTION('MONTH', e.expenseDate)")
    List<Object[]> getMonthlyExpensesTrend(@Param("startDate") LocalDateTime startDate);

    // ====== VALIDATION QUERIES ======

    /**
     * Check if expense exists for case and receipt number
     */
    @Query("SELECT COUNT(e) > 0 FROM CaseExpense e WHERE e.legalCase.id = :caseId AND e.receiptNumber = :receiptNumber")
    boolean existsByLegalCaseIdAndReceiptNumber(@Param("caseId") Long caseId, @Param("receiptNumber") String receiptNumber);

    /**
     * Find duplicate expenses by amount and date
     */
    @Query("SELECT e FROM CaseExpense e WHERE e.legalCase.id = :caseId AND e.amount = :amount AND e.expenseDate = :expenseDate AND e.id != :excludeId")
    List<CaseExpense> findPotentialDuplicates(@Param("caseId") Long caseId, @Param("amount") BigDecimal amount, @Param("expenseDate") LocalDateTime expenseDate, @Param("excludeId") Long excludeId);

    // ====== BULK OPERATIONS ======

    /**
     * Update expense status in bulk - ✅ CORREGIDO: usar approvedByUser y approvedAt
     */
    @Query("UPDATE CaseExpense e SET e.status = :newStatus, e.approvedByUser.id = :reviewerId, e.approvedAt = :reviewedAt WHERE e.id IN :expenseIds")
    int bulkUpdateStatus(@Param("expenseIds") List<Long> expenseIds, @Param("newStatus") ExpenseStatus newStatus, @Param("reviewerId") Long reviewerId, @Param("reviewedAt") LocalDateTime reviewedAt);

    /**
     * Mark expenses as reimbursed
     */
    @Query("UPDATE CaseExpense e SET e.reimbursedAt = :reimbursedAt WHERE e.id IN :expenseIds AND e.status = 'APPROVED'")
    int markAsReimbursed(@Param("expenseIds") List<Long> expenseIds, @Param("reimbursedAt") LocalDateTime reimbursedAt);

    // ====== DASHBOARD METRICS ======

    /**
     * Get total expenses for current month - ✅ CORREGIDO: usar FUNCTION
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM CaseExpense e WHERE e.status = 'APPROVED' AND FUNCTION('YEAR', e.expenseDate) = FUNCTION('YEAR', CURRENT_DATE) AND FUNCTION('MONTH', e.expenseDate) = FUNCTION('MONTH', CURRENT_DATE)")
    BigDecimal getCurrentMonthExpenses();

    /**
     * Get total expenses for previous month - ✅ CORREGIDO: usar FUNCTION compatible con SQL Server
     */
    @Query(value = "SELECT COALESCE(SUM(e.amount), 0) FROM case_expense e WHERE e.status = 'APPROVED' " +
            "AND YEAR(e.expense_date) = YEAR(DATEADD(MONTH, -1, GETDATE())) " +
            "AND MONTH(e.expense_date) = MONTH(DATEADD(MONTH, -1, GETDATE()))",
            nativeQuery = true)
    BigDecimal getPreviousMonthExpenses();

    /**
     * Count expenses by status for dashboard
     */
    @Query("SELECT e.status, COUNT(e) FROM CaseExpense e GROUP BY e.status")
    List<Object[]> getExpenseCountByStatus();
}