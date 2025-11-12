package org.splitzy.expense.dto.response;

import org.splitzy.expense.entity.Expense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

//  Response DTO for expense details
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {

    private Long id;
    private String title;
    private String description;
    private BigDecimal totalAmount;
    private String currency;
    private Long paidByUserId;
    private LocalDate expenseDate;
    private Expense.ExpenseCategory category;
    private Expense.SplitType splitType;
    private Long groupId;
    private String notes;
    private String receiptUrl;
    private Expense.ExpenseStatus status;
    private List<SplitResponse> splits;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitResponse {
        private Long id;
        private Long userId;
        private BigDecimal amount;
        private BigDecimal percentage;
        private Integer shares;
        private Boolean isSettled;
        private BigDecimal settledAmount;
        private BigDecimal remainingAmount;
        private String notes;
    }
}
