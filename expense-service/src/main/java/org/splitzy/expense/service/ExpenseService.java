package org.splitzy.expense.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splitzy.common.exception.ValidationException;
import org.splitzy.expense.dto.request.CreateExpenseRequest;
import org.splitzy.expense.dto.response.ExpenseResponse;
import org.splitzy.expense.entity.Expense;
import org.splitzy.expense.entity.ExpenseSplit;
import org.splitzy.expense.repository.ExpenseRepository;
import org.splitzy.expense.repository.ExpenseSplitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final SplitCalculationService splitCalculationService;
    private final ExpenseEventPublisher expenseEventPublisher;

    public ExpenseResponse createExpense(CreateExpenseRequest request, Long requestingUsedId) {
        log.info("Creating expense: {} with total amount: {}", request.getTitle(), request.getTotalAmount());
        splitCalculationService.validateSplitRequests(Expense.builder().splitType(request.getSplitType()).totalAmount(request.getTotalAmount()).build(), request.getSplits());

        Expense expense = Expense.builder()
                .splitType(request.getSplitType())
                .totalAmount(request.getTotalAmount())
                .title(request.getTitle())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .category(request.getCategory())
                .paidByUserId(request.getPaidByUserId())
                .groupId(request.getGroupId())
                .status(Expense.ExpenseStatus.ACTIVE)
                .receiptUrl(request.getReceiptUrl())
                .expenseDate(request.getExpenseDate())
                .notes(request.getNotes())
                .build();

        List<ExpenseSplit> splits = splitCalculationService.calculateSplits(expense,  request.getSplits());
        for(ExpenseSplit split : splits) {
            expense.addSplit(split);
        }
        if(!expense.isSplitValid()){
            throw new ValidationException("Invalid expense");
        }

        Expense savedExpense = expenseRepository.save(expense);
        log.info("Expense created successfully with ID: {}", savedExpense.getId());
        expenseEventPublisher.publishExpenseCreated(savedExpense);
        return mapToResponse(savedExpense);
    }

    public ExpenseResponse getExpenseById(Long expenseId) {
        log.debug("Fetching expense with ID: {}", expenseId);

        Expense expense = expenseRepository.findById(expenseId).orElseThrow();
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .description(expense.getDescription())
                .totalAmount(expense.getTotalAmount())
                .currency(expense.getCurrency())
                .paidByUserId(expense.getPaidByUserId())
                .expenseDate(expense.getExpenseDate())
                .category(expense.getCategory())
                .splitType(expense.getSplitType())
                .groupId(expense.getGroupId())
                .notes(expense.getNotes())
                .receiptUrl(expense.getReceiptUrl())
                .status(expense.getStatus())
                .splits(expense.getSplits().stream()
                        .map(this::mapSplitToResponse)
                        .collect(Collectors.toList()))
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdateAt())
                .build();
    }

    private ExpenseResponse.SplitResponse mapSplitToResponse(ExpenseSplit split) {
        return ExpenseResponse.SplitResponse.builder()
                .id(split.getId())
                .userId(split.getUserId())
                .amount(split.getAmount())
                .percentage(split.getPercentage())
                .shares(split.getShares())
                .isSettled(split.getIsSettled())
                .settledAmount(split.getSettledAmount())
                .remainingAmount(split.getRemainingAmount())
                .notes(split.getNotes())
                .build();
    }
}
