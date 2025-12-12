package org.splitzy.expense.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

//  Response DTO for user balance information
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {

    private Long userId;
    private BigDecimal totalOwed;      // Total amount user owes to others
    private BigDecimal totalOwedBy;    // Total amount others owe to user
    private BigDecimal netBalance;     // Net balance (totalOwedBy - totalOwed)
    private String currency;
    private List<IndividualBalance> balances;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndividualBalance {
        private Long otherUserId;
        private String otherUserName;
        private BigDecimal amount;
        private String type; // "owes" or "owed_by"
        private String currency;
    }
}