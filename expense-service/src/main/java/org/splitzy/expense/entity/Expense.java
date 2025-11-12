package org.splitzy.expense.entity;

import org.splitzy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "expenses", indexes = {
        @Index(name = "idx_paid_by", columnList = "paid_by_user_id"),
        @Index(name = "idx_group", columnList = "group_id"),
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_expense_date", columnList = "expense_date"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "paid_by_user_id", nullable = false)
    private Long paidByUserId;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ExpenseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false)
    @Builder.Default
    private SplitType splitType = SplitType.EQUAL;

    @Column(name = "group_id")
    private Long groupId; // Optional: for group expenses

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ExpenseStatus status = ExpenseStatus.ACTIVE;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExpenseSplit> splits = new ArrayList<>();

    /**
     * Add a split to this expense
     */
    public void addSplit(ExpenseSplit split) {
        splits.add(split);
        split.setExpense(this);
    }

    /**
     * Remove a split from this expense
     */
    public void removeSplit(ExpenseSplit split) {
        splits.remove(split);
        split.setExpense(null);
    }

    /**
     * Calculate total split amount
     */
    public BigDecimal calculateTotalSplitAmount() {
        return splits.stream()
                .map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Validate that splits sum equals total amount
     */
    public boolean isSplitValid() {
        BigDecimal totalSplit = calculateTotalSplitAmount();
        return totalAmount.compareTo(totalSplit) == 0;
    }

    /**
     * Expense categories
     */
    public enum ExpenseCategory {
        FOOD_DINING("Food & Dining"),
        GROCERIES("Groceries"),
        HOUSING("Housing"),
        TRANSPORTATION("Transportation"),
        UTILITIES("Utilities"),
        SHOPPING("Shopping"),
        ENTERTAINMENT("Entertainment"),
        TRAVEL("Travel"),
        HEALTHCARE("Healthcare"),
        EDUCATION("Education"),
        INSURANCE("Insurance"),
        FINANCE("Finance & Investments"),
        GIFTS_DONATIONS("Gifts & Donations"),
        PETS("Pets"),
        FAMILY_KIDS("Family & Kids"),
        PERSONAL_MISC("Personal & Miscellaneous"),
        OTHER("Other");

        private final String displayName;

        ExpenseCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    //  Split types
    public enum SplitType {
        EQUAL("Equal Split"),                      // Everyone pays the same share
        PERCENTAGE("Percentage Split"),            // Split by percentage of total
        EXACT("Exact Amount Split"),               // Manually assign exact amounts
        SHARES("Share-based Split"),               // Based on number of shares
        WEIGHTED("Weighted Split"),                // Based on user-specific weights (like salary or usage)
        CUSTOM_RATIO("Custom Ratio Split"),        // Split by user-defined ratios
        ITEMIZED("Itemized Split"),                // Split based on specific items purchased
        UNEQUAL("Unequal Split"),                  // When shares differ manually but not by exact amount
        ADJUSTMENT("Adjustment / Reimbursement");  // Used for balancing or manual corrections

        private final String displayName;

        SplitType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    //  Expense status
    public enum ExpenseStatus {
        ACTIVE,      // Expense is active and unsettled
        SETTLED,     // All splits have been settled
        CANCELLED,   // Expense was cancelled
        DISPUTED     // Expense is under dispute or review
    }
}