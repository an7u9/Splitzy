package org.splitzy.expense.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

//  Event DTO for Kafka messaging
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseEvent {

    private String eventType; // CREATED, UPDATED, DELETED, SETTLED
    private Long expenseId;
    private String title;
    private BigDecimal totalAmount;
    private String currency;
    private Long paidByUserId;
    private List<Long> participantUserIds;
    private LocalDateTime eventTimestamp;
    private String notes;

    public enum EventType {
        EXPENSE_CREATED,
        EXPENSE_UPDATED,
        EXPENSE_DELETED,
        SPLIT_SETTLED
    }
}