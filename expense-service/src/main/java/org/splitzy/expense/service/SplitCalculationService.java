package org.splitzy.expense.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.security.oauthbearer.internals.secured.ValidateException;
import org.splitzy.expense.dto.request.CreateExpenseRequest;
import org.splitzy.expense.entity.Expense;
import org.splitzy.expense.entity.ExpenseSplit;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SplitCalculationService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public List<ExpenseSplit> calculateSplits(Expense expense, List<CreateExpenseRequest.SplitRequest> splitRequests) {
        log.debug("Calculating splits for expense according to type: {}", expense.getSplitType());

        return switch (expense.getSplitType()){
            case EQUAL -> calculateEqualSpllits(expense, splitRequests);
            case CUSTOM_RATIO -> calculateCustomRatioSplits(expense, splitRequests);
            case EXACT -> calculateExactSplits(expense, splitRequests);
            case ITEMIZED -> calculateItemizedSplits(expense, splitRequests);
            case ADJUSTMENT -> calculateAdjustmentSplits(expense, splitRequests);
            default -> throw new ValidateException("Unsupported expense type: " + expense.getSplitType());
        };
    }
    private List<ExpenseSplit> calculateEqualSpllits(Expense expense, List<CreateExpenseRequest.SplitRequest> splitRequests) {
        if(splitRequests.isEmpty()){
            throw new ValidateException("Atleast one participant is required for equal splits.");
        }

        List<ExpenseSplit> splits = new ArrayList<>();
        int participant = splitRequests.size();
        BigDecimal baseAmount = expense.getTotalAmount().divide(BigDecimal.valueOf(participant), SCALE, ROUNDING_MODE );

        BigDecimal totalCalculated = baseAmount.multiply(BigDecimal.valueOf(participant));
        BigDecimal difference = expense.getTotalAmount().subtract(totalCalculated);

        for(int i = 0;i<splitRequests.size();i++){
            CreateExpenseRequest.SplitRequest request = splitRequests.get(i);

            BigDecimal amount = (i == splitRequests.size() -1) ? baseAmount.add(difference) : baseAmount;


            splits.add(buildSplit(expense, request, amount, BigDecimal.valueOf(100.0/participant)));
            log.debug("Equal split calculated for user {}: {}", request.getUserId(), amount);
        }
        return splits;
    }

    private List<ExpenseSplit> calculateCustomRatioSplits(Expense expense, List<CreateExpenseRequest.SplitRequest> splitRequests) {
        int totalRatio = splitRequests.stream()
                .mapToInt(r -> Optional.ofNullable(r.getRatio()).orElse(0))
                .sum();

        if(totalRatio <= 0){
            throw new ValidateException("Total ratio must be greater than 0 for custom ration split");
        }

        List<ExpenseSplit> splits = new ArrayList<>();
        BigDecimal totalCalculated = BigDecimal.ZERO;

        for(int i = 0;i<splitRequests.size();i++){
            CreateExpenseRequest.SplitRequest request = splitRequests.get(i);
            if(request.getRatio() == null || request.getRatio() <= 0){
                throw new ValidateException("Each participant's ratio must be greater than 0");
            }
        BigDecimal amount = expense.getTotalAmount()
                .multiply(BigDecimal.valueOf(request.getRatio()))
                .divide(BigDecimal.valueOf(totalRatio), SCALE, ROUNDING_MODE);

        if(i == splitRequests.size() -1){
        amount = expense.getTotalAmount().subtract(totalCalculated);
        }
        }

    }

    private List<ExpenseSplit> calculateExactSplits(Expense expense, List<CreateExpenseRequest.SplitRequest> splitRequests) {
        BigDecimal totalAmount = splitRequests.stream().map(CreateExpenseRequest.SplitRequest::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (expense.getTotalAmount().compareTo(totalAmount) != 0) {
            throw new ValidateException("Sum of exact amounts does not match total. Expected: " + expense.getTotalAmount() +
                    ", but got: " + totalAmount);
        }

            List<ExpenseSplit> splits = new ArrayList<>();
        return splitRequests.stream()
                .map(req -> buildSplit(expense, req, req.getAmount(), calculatePercentage(expense, req.getAmount())))
                .peek(req -> log.debug("Exact split for user {}: {}", req.getUserId(), req.getAmount()))
                .collect(Collectors.toList());
    }

    public void validateSplitRequests(Expense expense, List<CreateExpenseRequest.SplitRequest> splitRequests) {
        if(splitRequests.isEmpty() || splitRequests == null){
            throw new ValidateException("Atleast one split is required.");
        }
    }

    private ExpenseSplit buildSplit(Expense expense, CreateExpenseRequest.SplitRequest request, BigDecimal amount, BigDecimal percentage) {
        return ExpenseSplit.builder()
                .expense(expense)
                .userId(request.getUserId())
                .amount(amount.setScale(SCALE, ROUNDING_MODE))
                .percentage(percentage != null ? percentage.setScale(SCALE, ROUNDING_MODE) : null)
                .isSettled(false)
                .settledAmount(BigDecimal.ZERO)
                .notes(request.getNotes())
                .build();
    }

    private BigDecimal calculatePercentage(Expense expense,  BigDecimal amount) {
        if(expense.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(expense.getTotalAmount(), SCALE + 2, ROUNDING_MODE);
    }
}
