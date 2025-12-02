package org.splitzy.expense.entity;

import org.splitzy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ExpenseSplit entity representing individual user's share in an expense
 */
@Entity
@Table(name = "expense_splits", indexes = {
        @Index(name = "idx_expense", columnList = "expense_id"),
        @Index(name = "idx_user", columnList = "user_id"),
        @Index(name = "idx_paid_status", columnList = "is_settled"),
        @Index(name = "idx_expense_user", columnList = "expense_id, user_id", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true, exclude = "expense")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSplit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage; // For percentage-based splits

    @Column(name = "shares")
    private Integer shares; // For share-based splits

    @Column(name = "is_settled", nullable = false)
    @Builder.Default
    private Boolean isSettled = Boolean.FALSE;

    @Column(name = "settled_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal settledAmount = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "Total Items", length = 500)
    private BigDecimal itemTotal;

    @Column(name = "Adjsutments", length = 500)
    private BigDecimal adjustment;

    /**
     * Check if user owes money (not the payer)
     */
    public boolean isOwing() {
        return !userId.equals(expense.getPaidByUserId());
    }

    /**
     * Get remaining amount to be settled
     */
    public BigDecimal getRemainingAmount() {
        return amount.subtract(settledAmount);
    }

    /**
     * Mark split as settled
     */
    public void markAsSettled() {
        this.isSettled = Boolean.TRUE;
        this.settledAmount = this.amount;
    }

    /**
     * Partially settle the split
     */
    public void partiallySettle(BigDecimal partialAmount) {
        if (partialAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Partial amount must be positive");
        }

        BigDecimal newSettledAmount = (this.settledAmount != null ? this.settledAmount : BigDecimal.ZERO).add(partialAmount);
        if (newSettledAmount.compareTo(this.amount) > 0) {
            throw new IllegalArgumentException("Settled amount cannot exceed total split amount");
        }

        this.settledAmount = newSettledAmount;
        this.isSettled = this.settledAmount.compareTo(this.amount) == 0;
    }
}