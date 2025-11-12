package org.splitzy.expense.entity;

import org.splitzy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "user_balances", indexes = {
        @Index(name = "idx_user1", columnList = "user1_id"),
        @Index(name = "idx_user2", columnList = "user2_id"),
        @Index(name = "idx_users_pair", columnList = "user1_id, user2_id", unique = true)
})
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBalance extends BaseEntity {

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @Column(name = "balance_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    /**
     * Check if balance is settled (zero)
     */
    public boolean isSettled() {
        return balanceAmount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Get who owes whom
     * Returns positive value if user1 owes user2
     * Returns negative value if user2 owes user1
     */
    public BigDecimal getOwedAmount() {
        return balanceAmount;
    }

    /**
     * Update balance with new amount
     * Positive amount means user1 owes more to user2
     */
    public void updateBalance(BigDecimal amount) {
        this.balanceAmount = this.balanceAmount.add(amount);
    }

    public boolean isPositive() {
        return balanceAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return balanceAmount.compareTo(BigDecimal.ZERO) < 0;
    }

    public void settle() {
        this.balanceAmount = BigDecimal.ZERO;
    }

    /**
     * Partially settle the balance
     */
    public void partiallySettle(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Settlement amount must be positive");
        }

        if (balanceAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.balanceAmount = this.balanceAmount.subtract(amount);
        } else {
            this.balanceAmount = this.balanceAmount.add(amount);
        }
    }
}
