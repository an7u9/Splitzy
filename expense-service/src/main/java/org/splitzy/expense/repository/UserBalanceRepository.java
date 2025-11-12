package org.splitzy.expense.repository;

import org.splitzy.expense.entity.UserBalance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

// Repository for UserBalance entity operations
@Repository
public interface UserBalanceRepository extends JpaRepository<UserBalance, Long> {

    //  Find balance between two users
    @Query("SELECT b FROM UserBalance b WHERE (b.user1Id = :user1Id AND b.user2Id = :user2Id) OR (b.user1Id = :user2Id AND b.user2Id = :user1Id)")
    Optional<UserBalance> findBalanceBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    // Find all balances for a user
    @Query("SELECT b FROM UserBalance b WHERE (b.user1Id = :userId OR b.user2Id = :userId) AND b.balanceAmount != 0 ORDER BY b.balanceAmount DESC")
    List<UserBalance> findUserBalances(@Param("userId") Long userId);

    // Find paginated balances for a user
    @Query("SELECT b FROM UserBalance b WHERE (b.user1Id = :userId OR b.user2Id = :userId) AND b.balanceAmount != 0")
    Page<UserBalance> findUserBalancesPaginated(@Param("userId") Long userId, Pageable pageable);

     // Calculate total amount user owes to others
    @Query("SELECT COALESCE(SUM(b.balanceAmount), 0) FROM UserBalance b WHERE b.user1Id = :userId AND b.balanceAmount > 0")
    BigDecimal calculateTotalOwedByUser(@Param("userId") Long userId);

    // Calculate total amount others owe to user
    @Query("SELECT COALESCE(SUM(ABS(b.balanceAmount)), 0) FROM UserBalance b WHERE b.user2Id = :userId AND b.balanceAmount < 0")
    BigDecimal calculateTotalOwedToUser(@Param("userId") Long userId);

    // Update balance
    @Modifying
    @Query("UPDATE UserBalance b SET b.balanceAmount = b.balanceAmount + :amount WHERE b.id = :balanceId")
    void updateBalance(@Param("balanceId") Long balanceId, @Param("amount") BigDecimal amount);

    // Settle balance
    @Modifying
    @Query("UPDATE UserBalance b SET b.balanceAmount = 0 WHERE b.id = :balanceId")
    void settleBalance(@Param("balanceId") Long balanceId);

    // Find unsettled balances
    @Query("SELECT b FROM UserBalance b WHERE (b.user1Id = :userId OR b.user2Id = :userId) AND b.balanceAmount != 0")
    List<UserBalance> findUnsettledBalances(@Param("userId") Long userId);

    // Check if balance exists between users
    @Query("SELECT COUNT(b) > 0 FROM UserBalance b WHERE (b.user1Id = :user1Id AND b.user2Id = :user2Id) OR (b.user1Id = :user2Id AND b.user2Id = :user1Id)")
    boolean balanceExistsBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}