package org.splitzy.expense.dto;

import org.splitzy.expense.entity.Expense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

//  DTO for expense search criteria
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSearchCriteria {

    private Long userId;
    private Long paidByUserId;
    private Long groupId;
    private List<Expense.ExpenseCategory> categories;
    private Expense.SplitType splitType;
    private Expense.ExpenseStatus status;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private BigDecimal amountMin;
    private BigDecimal amountMax;
    private String searchTerm; // Search in title and description

    // Pagination
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}