package org.splitzy.expense.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splitzy.expense.dto.ExpenseEvent;
import org.splitzy.expense.entity.Expense;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseEventPublisher {

    private final KafkaTemplate<String, ExpenseEvent> kafkaTemplate;
    private static final String EXPENSE_TOPIC = "expense-events";

    public void publishExpenseCreated(Expense expense) {
        ExpenseEvent event = buildExpenseEvent(ExpenseEvent.EventType.EXPENSE_CREATED.name(), expense);
        publishEvent(event);
        log.info("Published EXPENSE_CREATED event for expense: {}", expense.getId());
    }

    public void publishExpenseUpdated(Expense expense) {
        ExpenseEvent event = buildExpenseEvent(ExpenseEvent.EventType.EXPENSE_UPDATED.name(), expense);
        publishEvent(event);
        log.info("Published EXPENSE_UPDATED event for expense: {}", expense.getId());
    }

    public void publishExpenseDeleted(Expense expense) {
        ExpenseEvent event = buildExpenseEvent(ExpenseEvent.EventType.EXPENSE_DELETED.name(), expense);
        publishEvent(event);
        log.info("Published EXPENSE_DELETED event for expense: {}", expense.getId());
    }

    public void publishSplitSettled(Expense expense) {
        ExpenseEvent event = buildExpenseEvent(ExpenseEvent.EventType.SPLIT_SETTLED.name(),  expense);
        publishEvent(event);
        log.info("Published EXPENSE_SETTLED event for expense: {}", expense.getId());
    }

    private void publishEvent(ExpenseEvent event) {
        try{
            kafkaTemplate.send(EXPENSE_TOPIC, event.getExpenseId().toString(), event);
            log.debug("Event published successfully to topic: {}", EXPENSE_TOPIC);
        } catch (Exception e){
            log.error("Failed to publish event to kafka: {}", e.getMessage(), e);
        }
    }

    private ExpenseEvent buildExpenseEvent(String evetType, Expense expense) {
        return ExpenseEvent.builder()
                .eventType(evetType)
                .expenseId(expense.getId())
                .currency(expense.getCurrency())
                .participantUserIds(expense.getSplits().stream()
                        .map(split -> split.getUserId())
                        .collect(Collectors.toList()))
                .notes(expense.getNotes())
                .paidByUserId(expense.getPaidByUserId())
                .totalAmount(expense.getTotalAmount())
                .eventTimestamp(LocalDateTime.now())
                .build();
    }
}
