package org.splitzy.expense.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splitzy.common.exception.ResourceNotFoundException;
import org.splitzy.expense.dto.response.BalanceResponse;
import org.splitzy.expense.entity.UserBalance;
import org.splitzy.expense.repository.UserBalanceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BalanceService {
    private final UserBalanceRepository userBalanceRepository;

    /**
     * Get balance for a specific pair of users
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalanceBetweenUsers(Long user1Id, Long user2Id) {
        log.debug("Fetching balance between users: {} and {}", user1Id, user2Id);

        Optional<UserBalance> balance = userBalanceRepository.findBalanceBetweenUsers(user1Id, user2Id);

        if (balance.isEmpty()) {
            // No balance record exists, create default zero balance
            return createDefaultBalance(user1Id, user2Id);
        }

        UserBalance userBalance = balance.get();
        return mapToResponse(userBalance, user1Id);
    }

    /**
     * Get all balances for a user
     */
    @Transactional(readOnly = true)
    public BalanceResponse getUserBalances(Long userId) {
        log.debug("Fetching all balances for user: {}", userId);

        List<UserBalance> balances = userBalanceRepository.findUserBalances(userId);

        BigDecimal totalOwed = userBalanceRepository.calculateTotalOwedByUser(userId);
        BigDecimal totalOwedBy = userBalanceRepository.calculateTotalOwedToUser(userId);
        BigDecimal netBalance = totalOwedBy.subtract(totalOwed);

        List<BalanceResponse.IndividualBalance> individualBalances = balances.stream()
                .map(b -> mapToIndividualBalance(b, userId))
                .collect(Collectors.toList());

        return BalanceResponse.builder()
                .userId(userId)
                .totalOwed(totalOwed)
                .totalOwedBy(totalOwedBy)
                .netBalance(netBalance)
                .currency("INR")
                .balances(individualBalances)
                .build();
    }

    /**
     * Get paginated balances for a user
     */
    @Transactional(readOnly = true)
    public Page<BalanceResponse.IndividualBalance> getUserBalancesPaginated(Long userId, int page, int size) {
        log.debug("Fetching paginated balances for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<UserBalance> balances = userBalanceRepository.findUserBalancesPaginated(userId, pageable);

        return balances.map(b -> mapToIndividualBalance(b, userId));
    }

    /**
     * Update or create balance between two users
     */
    public void updateBalance(Long user1Id, Long user2Id, BigDecimal amount) {
        log.debug("Updating balance between users {} and {} with amount: {}", user1Id, user2Id, amount);

        Optional<UserBalance> existingBalance = userBalanceRepository.findBalanceBetweenUsers(user1Id, user2Id);

        if (existingBalance.isPresent()) {
            UserBalance balance = existingBalance.get();
            balance.updateBalance(amount);
            userBalanceRepository.save(balance);
        } else {
            // Create new balance record
            UserBalance newBalance = UserBalance.builder()
                    .user1Id(user1Id)
                    .user2Id(user2Id)
                    .balanceAmount(amount)
                    .currency("INR")
                    .build();
            userBalanceRepository.save(newBalance);
        }

        log.debug("Balance updated successfully");
    }

    /**
     * Settle balance between two users
     */
    public void settleBalance(Long user1Id, Long user2Id) {
        log.info("Settling balance between users: {} and {}", user1Id, user2Id);

        Optional<UserBalance> balance = userBalanceRepository.findBalanceBetweenUsers(user1Id, user2Id);
        if (balance.isPresent()) {
            balance.get().settle();
            userBalanceRepository.save(balance.get());
            log.info("Balance settled successfully");
        }
    }

    /**
     * Partially settle balance
     */
    public void partiallySettleBalance(Long user1Id, Long user2Id, BigDecimal amount) {
        log.info("Partially settling balance between users: {} and {} with amount: {}", user1Id, user2Id, amount);

        Optional<UserBalance> balance = userBalanceRepository.findBalanceBetweenUsers(user1Id, user2Id);
        if (balance.isPresent()) {
            balance.get().partiallySettle(amount);
            userBalanceRepository.save(balance.get());
            log.info("Balance partially settled successfully");
        } else {
            throw new ResourceNotFoundException("Balance not found between users");
        }
    }

    /**
     * Create default zero balance response
     */
    private BalanceResponse createDefaultBalance(Long user1Id, Long user2Id) {
        return BalanceResponse.builder()
                .userId(user1Id)
                .totalOwed(BigDecimal.ZERO)
                .totalOwedBy(BigDecimal.ZERO)
                .netBalance(BigDecimal.ZERO)
                .currency("INR")
                .balances(new ArrayList<>())
                .build();
    }

    /**
     * Map UserBalance to BalanceResponse
     */
    private BalanceResponse mapToResponse(UserBalance userBalance, Long userId) {
        BigDecimal totalOwed = userBalanceRepository.calculateTotalOwedByUser(userId);
        BigDecimal totalOwedBy = userBalanceRepository.calculateTotalOwedToUser(userId);

        List<BalanceResponse.IndividualBalance> balances = new ArrayList<>();
        balances.add(mapToIndividualBalance(userBalance, userId));

        return BalanceResponse.builder()
                .userId(userId)
                .totalOwed(totalOwed)
                .totalOwedBy(totalOwedBy)
                .netBalance(totalOwedBy.subtract(totalOwed))
                .currency("INR")
                .balances(balances)
                .build();
    }

    /**
     * Map UserBalance to IndividualBalance
     */
    private BalanceResponse.IndividualBalance mapToIndividualBalance(UserBalance balance, Long userId) {
        if (balance.getUser1Id().equals(userId)) {
            String type = balance.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0 ? "owes" : "owed_by";
            return BalanceResponse.IndividualBalance.builder()
                    .otherUserId(balance.getUser2Id())
                    .amount(balance.getBalanceAmount().abs())
                    .type(type)
                    .currency("INR")
                    .build();
        } else {
            // Reverse the amount and type for user2
            BigDecimal reversedAmount = balance.getBalanceAmount().negate();
            String type = reversedAmount.compareTo(BigDecimal.ZERO) > 0 ? "owes" : "owed_by";
            return BalanceResponse.IndividualBalance.builder()
                    .otherUserId(balance.getUser1Id())
                    .amount(reversedAmount.abs())
                    .type(type)
                    .currency("INR")
                    .build();
        }
    }
}
