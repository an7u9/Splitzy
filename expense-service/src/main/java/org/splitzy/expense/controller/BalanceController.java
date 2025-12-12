package org.splitzy.expense.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splitzy.common.dto.ApiResponse;
import org.splitzy.expense.dto.response.BalanceResponse;
import org.splitzy.expense.service.BalanceService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/balances")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    /**
     * Get all balances for user
     */
    @GetMapping
    @Operation(summary = "Get all balances", description = "Get all balances and settlement status for user")
    public ResponseEntity<ApiResponse<BalanceResponse>> getUserBalances(@RequestHeader("X-User-Id") Long userId) {
        log.info("Get balances request for user: {}", userId);

        BalanceResponse response = balanceService.getUserBalances(userId);
        ApiResponse<BalanceResponse> apiResponse = ApiResponse.success(response);

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get balance between two users
     */
    @GetMapping("/between/{otherUserId}")
    @Operation(summary = "Get balance between users", description = "Get balance and settlement status between two users")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalanceBetweenUsers(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long otherUserId) {
        log.info("Get balance between users: {} and {}", userId, otherUserId);

        BalanceResponse response = balanceService.getBalanceBetweenUsers(userId, otherUserId);
        ApiResponse<BalanceResponse> apiResponse = ApiResponse.success(response);

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get paginated balances
     */
    @GetMapping("/paginated")
    @Operation(summary = "Get paginated balances", description = "Get user balances with pagination")
    public ResponseEntity<ApiResponse<Page<BalanceResponse.IndividualBalance>>> getPaginatedBalances(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("Get paginated balances for user: {}", userId);

        Page<BalanceResponse.IndividualBalance> response = balanceService.getUserBalancesPaginated(userId, page, size);
        ApiResponse<Page<BalanceResponse.IndividualBalance>> apiResponse = ApiResponse.success(response);

        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if balance service is running")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        ApiResponse<String> apiResponse = ApiResponse.success("Balance service is running");
        return ResponseEntity.ok(apiResponse);
    }
}