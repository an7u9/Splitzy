package org.splitzy.expense.repository;

import org.splitzy.expense.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// Repository for ExpenseSplit entity operations
@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    // Find splits by expense
    List<ExpenseSplit> findByExpenseId(Long expenseId);

    // Find splits by user (amounts they owe)
    List<ExpenseSplit> findByUserIdAndIsSettledFalse(Long userId);

    // Find all splits for a user in a specific expense
    Optional<ExpenseSplit> findByExpenseIdAndUserId(Long expenseId, Long userId);

    // Calculate total unsettled amount for user
    @Query("SELECT COALESCE(SUM(s.amount - s.settledAmount), 0) FROM ExpenseSplit s WHERE s.userId = :userId AND s.isSettled = false")
    BigDecimal calculateTotalUnsettledAmount(@Param("userId") Long userId);

    // Find unsettled splits between two users
    @Query("SELECT s FROM ExpenseSplit s WHERE s.userId = :userId AND s.isSettled = false AND s.expense.paidByUserId = :otherUserId")
    List<ExpenseSplit> findUnsettledSplitsBetweenUsers(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    // Mark split as settled
    @Modifying
    @Query("UPDATE ExpenseSplit s SET s.isSettled = true, s.settledAmount = s.amount WHERE s.id = :splitId")
    void markAsSettled(@Param("splitId") Long splitId);

    // Partially settle split
    @Modifying
    @Query("UPDATE ExpenseSplit s SET s.settledAmount = s.settledAmount + :amount WHERE s.id = :splitId")
    void partiallySettle(@Param("splitId") Long splitId, @Param("amount") BigDecimal amount);

    // Count unsettled splits for user
    @Query("SELECT COUNT(s) FROM ExpenseSplit s WHERE s.userId = :userId AND s.isSettled = false")
    long countUnsettledSplits(@Param("userId") Long userId);

    // Find all splits for expense
    @Query("SELECT s FROM ExpenseSplit s WHERE s.expense.id = :expenseId ORDER BY s.userId")
    List<ExpenseSplit> findAllSplitsForExpense(@Param("expenseId") Long expenseId);
}