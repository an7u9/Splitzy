package org.splitzy.expense.repository;

import org.splitzy.expense.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Repository for Expense entity operations
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    // Find expenses by paid by user
    Page<Expense> findByPaidByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    // Find all expenses for a user (either paid by or involved in)
    @Query("SELECT e FROM Expense e WHERE e.isActive = true AND (e.paidByUserId = :userId OR EXISTS (SELECT 1 FROM ExpenseSplit s WHERE s.expense = e AND s.userId = :userId))")
    Page<Expense> findUserExpenses(@Param("userId") Long userId, Pageable pageable);

    // Find expenses by group
    Page<Expense> findByGroupIdAndIsActiveTrue(Long groupId, Pageable pageable);

    // Find unsettled expenses for a user
    @Query("SELECT DISTINCT e FROM Expense e JOIN e.splits s WHERE e.isActive = true AND s.isSettled = false AND (e.paidByUserId = :userId OR s.userId = :userId)")
    Page<Expense> findUnsettledExpenses(@Param("userId") Long userId, Pageable pageable);

    // Find expenses created between dates
    Page<Expense> findByExpenseDateBetweenAndIsActiveTrue(LocalDate startDate, LocalDate endDate, Pageable pageable);

    // Find expenses by category
    Page<Expense> findByCategoryAndIsActiveTrue(Expense.ExpenseCategory category, Pageable pageable);

    // Find expenses by status
    Page<Expense> findByStatusAndIsActiveTrue(Expense.ExpenseStatus status, Pageable pageable);

    // Calculate total expenses for user
    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expense e WHERE e.paidByUserId = :userId AND e.isActive = true")
    BigDecimal calculateTotalExpensesPaidByUser(@Param("userId") Long userId);

    // Calculate total amount split with user
    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM ExpenseSplit s WHERE s.userId = :userId AND s.isSettled = false AND s.expense.isActive = true")
    BigDecimal calculateTotalUnsettledAmount(@Param("userId") Long userId);

    // Find expenses involving both users
    @Query("SELECT e FROM Expense e WHERE e.isActive = true AND e.paidByUserId = :userId1 AND EXISTS (SELECT 1 FROM ExpenseSplit s WHERE s.expense = e AND s.userId = :userId2)")
    Page<Expense> findExpensesBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2, Pageable pageable);

    // Find recent expenses
    @Query("SELECT e FROM Expense e WHERE e.isActive = true ORDER BY e.expenseDate DESC, e.createdAt DESC")
    Page<Expense> findRecentExpenses(Pageable pageable);

    // Mark expenses as settled
    @Modifying
    @Query("UPDATE Expense e SET e.status = 'SETTLED' WHERE e.id = :expenseId")
    void markAsSettled(@Param("expenseId") Long expenseId);

    // Soft delete expense
    @Modifying
    @Query("UPDATE Expense e SET e.isActive = false WHERE e.id = :expenseId")
    void softDelete(@Param("expenseId") Long expenseId);

    // Find expenses for dashboard (last 30 days)
    @Query("SELECT e FROM Expense e WHERE e.isActive = true AND (e.paidByUserId = :userId OR EXISTS (SELECT 1 FROM ExpenseSplit s WHERE s.expense = e AND s.userId = :userId)) AND e.expenseDate >= CURRENT_DATE - 30 ORDER BY e.expenseDate DESC")
    List<Expense> findDashboardExpenses(@Param("userId") Long userId);
}