package org.splitzy.expense.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import org.splitzy.common.dto.ApiResponse;
import org.splitzy.common.dto.PageResponse;
import org.splitzy.expense.dto.ExpenseSearchCriteria;
import org.splitzy.expense.dto.request.CreateExpenseRequest;
import org.splitzy.expense.dto.request.UpdateExpenseRequest;
import org.splitzy.expense.dto.response.ExpenseResponse;
import org.splitzy.expense.dto.response.ExpenseStatisticsResponse;
import org.splitzy.expense.service.ExpenseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    /** Create a new expense */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Create expense request from user: {}", userId);

        ExpenseResponse response = expenseService.createExpense(request, userId);
        ApiResponse<ExpenseResponse> apiResponse = ApiResponse.success(response, "Expense created successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    /** Get expense by ID */
    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpense(
            @PathVariable Long expenseId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Get expense request for ID: {}", expenseId);

        ExpenseResponse response = expenseService.getExpenseById(expenseId);
        ApiResponse<ExpenseResponse> apiResponse = ApiResponse.success(response);

        return ResponseEntity.ok(apiResponse);
    }

    /** Update expense */
    @PutMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable Long expenseId,
            @Valid @RequestBody UpdateExpenseRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Update expense request for ID: {}", expenseId);

        ExpenseResponse response = expenseService.updateExpense(expenseId, request, userId);
        ApiResponse<ExpenseResponse> apiResponse = ApiResponse.success(response, "Expense updated successfully");

        return ResponseEntity.ok(apiResponse);
    }

    /** Get user's expenses with filtering */
    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseResponse>>> getUserExpenses(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        log.info("Get expenses request for user: {}", userId);

        ExpenseSearchCriteria criteria = ExpenseSearchCriteria.builder()
                .userId(userId)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .searchTerm(searchTerm)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build();

        PageResponse<ExpenseResponse> response = expenseService.getUserExpenses(userId, criteria);
        ApiResponse<PageResponse<ExpenseResponse>> apiResponse = ApiResponse.success(response);

        return ResponseEntity.ok(apiResponse);
    }

    /** Delete expense */
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<String>> deleteExpense(
            @PathVariable Long expenseId,
            @RequestHeader("X-User-Id") Long userId) {
        log.info("Delete expense request for ID: {}", expenseId);

        expenseService.deleteExpense(expenseId, userId);
        ApiResponse<String> apiResponse = ApiResponse.success("Expense deleted successfully");

        return ResponseEntity.ok(apiResponse);
    }

    /** Get expense statistics */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<ExpenseStatisticsResponse>> getStatistics(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Get statistics request for user: {} from {} to {}", userId, startDate, endDate);

        ExpenseStatisticsResponse response = expenseService.getExpenseStatistics(userId, startDate, endDate);
        ApiResponse<ExpenseStatisticsResponse> apiResponse = ApiResponse.success(response);

        return ResponseEntity.ok(apiResponse);
    }
}