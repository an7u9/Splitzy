package org.splitzy.expense.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splitzy.common.criteria.GenericCriteriaBuilder;
import org.splitzy.common.criteria.SearchCriteria;
import org.splitzy.common.criteria.SearchOperation;
import org.splitzy.common.dto.PageResponse;
import org.splitzy.common.exception.ResourceNotFoundException;
import org.splitzy.common.exception.ValidationException;
import org.splitzy.expense.dto.ExpenseSearchCriteria;
import org.splitzy.expense.dto.request.CreateExpenseRequest;
import org.splitzy.expense.dto.request.UpdateExpenseRequest;
import org.splitzy.expense.dto.response.ExpenseResponse;
import org.splitzy.expense.dto.response.ExpenseStatisticsResponse;
import org.splitzy.expense.entity.Expense;
import org.splitzy.expense.entity.ExpenseSplit;
import org.splitzy.expense.repository.ExpenseRepository;
import org.splitzy.expense.repository.ExpenseSplitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long expenseId) {
        log.debug("Fetching expense with ID: {}", expenseId);

        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));
        return mapToResponse(expense);
    }

    // Update expense
    public ExpenseResponse updateExpense(Long expenseId, UpdateExpenseRequest request, Long requestingUserId) {
        log.info("Updating expense with ID: {}", expenseId);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));

        // Only allow updating non-settled expenses
        if (expense.getStatus() == Expense.ExpenseStatus.SETTLED) {
            throw new ValidationException("Cannot update a settled expense");
        }

        // Update fields
        if (request.getTitle() != null) {
            expense.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            expense.setDescription(request.getDescription());
        }
        if (request.getExpenseDate() != null) {
            expense.setExpenseDate(request.getExpenseDate());
        }
        if (request.getCategory() != null) {
            expense.setCategory(request.getCategory());
        }
        if (request.getNotes() != null) {
            expense.setNotes(request.getNotes());
        }
        if (request.getReceiptUrl() != null) {
            expense.setReceiptUrl(request.getReceiptUrl());
        }
        if (request.getStatus() != null) {
            expense.setStatus(request.getStatus());
        }

        Expense updatedExpense = expenseRepository.save(expense);
        log.info("Expense updated successfully with ID: {}", expenseId);

        // Publish event
        expenseEventPublisher.publishExpenseUpdated(updatedExpense);

        return mapToResponse(updatedExpense);
    }

    //  Get user's expenses with dynamic filtering
    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> getUserExpenses(Long userId, ExpenseSearchCriteria criteria) {
        log.debug("Fetching expenses for user: {} with criteria: {}", userId, criteria);

        // Build specifications
        List<SearchCriteria> searchCriteria = new ArrayList<>();

        if (criteria.getCategories() != null && !criteria.getCategories().isEmpty()) {
            searchCriteria.add(new SearchCriteria("category", SearchOperation.IN, criteria.getCategories()));
        }

        if (criteria.getStatus() != null) {
            searchCriteria.add(new SearchCriteria("status", SearchOperation.EQUALITY, criteria.getStatus()));
        }

        if (criteria.getDateFrom() != null) {
            searchCriteria.add(new SearchCriteria("expenseDate", SearchOperation.GREATER_THAN_OR_EQUAL, criteria.getDateFrom()));
        }

        if (criteria.getDateTo() != null) {
            searchCriteria.add(new SearchCriteria("expenseDate", SearchOperation.LESS_THAN_OR_EQUAL, criteria.getDateTo()));
        }

        if (criteria.getAmountMin() != null) {
            searchCriteria.add(new SearchCriteria("totalAmount", SearchOperation.GREATER_THAN_OR_EQUAL, criteria.getAmountMin()));
        }

        if (criteria.getAmountMax() != null) {
            searchCriteria.add(new SearchCriteria("totalAmount", SearchOperation.LESS_THAN_OR_EQUAL, criteria.getAmountMax()));
        }

        if (criteria.getSearchTerm() != null && !criteria.getSearchTerm().isBlank()) {
            searchCriteria.add(new SearchCriteria("title", SearchOperation.LIKE, criteria.getSearchTerm()));
        }

        searchCriteria.add(new SearchCriteria("isActive", SearchOperation.EQUALITY, true));

        // Build sort
        Sort sort = Sort.by(Sort.Direction.DESC, "expenseDate");
        if (criteria.getSortBy() != null && !criteria.getSortBy().isBlank()) {
            Sort.Direction direction = "ASC".equalsIgnoreCase(criteria.getSortDirection())
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            sort = Sort.by(direction, criteria.getSortBy());
        }

        Pageable pageable = PageRequest.of(
                criteria.getPage() != null ? criteria.getPage() : 0,
                criteria.getSize() != null ? criteria.getSize() : 10,
                sort
        );

        Specification<Expense> spec = GenericCriteriaBuilder.of(searchCriteria).build();
        Page<Expense> expenses = expenseRepository.findUserExpenses(userId, pageable);

        return PageResponse.of(expenses.map(this::mapToResponse));
    }

    /** Get unsettled expenses for user */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getUnsettledExpenses(Long userId) {
        log.debug("Fetching unsettled expenses for user: {}", userId);

        Pageable pageable = PageRequest.of(0, 1000);
        Page<Expense> unsettledExpenses = expenseRepository.findUnsettledExpenses(userId, pageable);

        return unsettledExpenses.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Delete expense (soft delete) */
    public void deleteExpense(Long expenseId, Long requestingUserId) {
        log.info("Deleting expense with ID: {}", expenseId);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));

        expense.setIsActive(false);
        expenseRepository.save(expense);

        // Publish event
        expenseEventPublisher.publishExpenseDeleted(expense);

        log.info("Expense deleted successfully with ID: {}", expenseId);
    }

    /**
     * Get expense statistics for user
     */
    @Transactional(readOnly = true)
    public ExpenseStatisticsResponse getExpenseStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating expense statistics for user: {} from {} to {}", userId, startDate, endDate);

        Page<Expense> expenses = expenseRepository.findByExpenseDateBetweenAndIsActiveTrue(
                startDate, endDate, PageRequest.of(0, Integer.MAX_VALUE)
        );

        BigDecimal totalPaid = expenses.getContent().stream()
                .filter(e -> e.getPaidByUserId().equals(userId))
                .map(Expense::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOwed = expenses.getContent().stream()
                .flatMap(e -> e.getSplits().stream())
                .filter(s -> s.getUserId().equals(userId) && !s.getIsSettled())
                .map(ExpenseSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ExpenseStatisticsResponse.builder()
                .userId(userId)
                .totalPaid(totalPaid)
                .totalOwed(totalOwed)
                .netBalance(totalPaid.subtract(totalOwed))
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();
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
