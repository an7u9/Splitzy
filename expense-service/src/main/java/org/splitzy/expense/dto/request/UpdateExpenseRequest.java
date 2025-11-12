package org.splitzy.expense.dto.request;

import org.splitzy.expense.entity.Expense;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Request DTO for updating an existing expense
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateExpenseRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @PastOrPresent(message = "Expense date cannot be in the future")
    private LocalDate expenseDate;

    private Expense.ExpenseCategory category;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @Size(max = 500, message = "Receipt URL must not exceed 500 characters")
    private String receiptUrl;

    private Expense.ExpenseStatus status;
}