package org.splitzy.expense.dto.request;

import org.splitzy.expense.entity.Expense;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 *G Request DTO for creating a new expense
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
    private BigDecimal totalAmount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    private String currency;

    @NotNull(message = "Paid by user ID is required")
    @Positive(message = "Paid by user ID must be positive")
    private Long paidByUserId;

    @NotNull(message = "Expense date is required")
    @PastOrPresent(message = "Expense date cannot be in the future")
    private LocalDate expenseDate;

    @NotNull(message = "Category is required")
    private Expense.ExpenseCategory category;

    @NotNull(message = "Split type is required")
    private Expense.SplitType splitType;

    private Long groupId;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @Size(max = 500, message = "Receipt URL must not exceed 500 characters")
    private String receiptUrl;

    @NotNull(message = "Splits are required")
    @Size(min = 1, message = "At least one split is required")
    @Valid
    private List<SplitRequest> splits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitRequest {

        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be positive")
        private Long userId;

        @DecimalMin(value = "0.0", message = "Amount must be non-negative")
        @Digits(integer = 10, fraction = 2, message = "Invalid amount format")
        private BigDecimal amount;

        @DecimalMin(value = "0.0", message = "Percentage must be non-negative")
        @DecimalMax(value = "100.0", message = "Percentage cannot exceed 100")
        @Digits(integer = 3, fraction = 2, message = "Invalid percentage format")
        private BigDecimal percentage;

        @DecimalMin(value = "0.00", message = "Item total cannot be negative")
        @Digits(integer = 12, fraction = 2, message = "Invalid item total format")
        private BigDecimal itemTotal;

        @Min(value = 1, message = "Shares must be at least 1")
        private Integer shares;

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        private String notes;

        @Min(value = 1, message = "Ratio must be at least 1")
        private Integer ratio;

        private BigDecimal adjustment;
    }

    //  Validate that splits sum equals total amount (for EXACT split type)
    public boolean isSplitsValid() {
        if (splitType == Expense.SplitType.EXACT || splitType == Expense.SplitType.ITEMIZED) {
            BigDecimal totalSplit = splits.stream()
                    .map(req -> {
                        if(splitType == Expense.SplitType.ITEMIZED) { return req.getItemTotal();}
                        if(splitType == Expense.SplitType.EXACT){return req.getAmount();}
                        return BigDecimal.ZERO;
                    })
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return totalAmount.compareTo(totalSplit) == 0;
        }
        return true;
    }
}