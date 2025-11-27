package com.trading.hyperliquid.controller;

import com.trading.hyperliquid.model.dto.response.ApiResponse;
import com.trading.hyperliquid.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "Account Operations", description = "Hyperliquid account operations - positions, orders, cancellations")
@SecurityRequirement(name = "Bearer Authentication")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{userId}/positions")
    @Operation(summary = "Get positions", description = "Get current positions for a user account on Hyperliquid")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPositions(
            @Parameter(description = "User ID") @PathVariable Long userId
    ) {
        Map<String, Object> positions = accountService.getPositions(userId);
        return ResponseEntity.ok(ApiResponse.success("Positions retrieved successfully", positions));
    }

    @GetMapping("/{userId}/open-orders")
    @Operation(summary = "Get open orders", description = "Get all open orders for a user account on Hyperliquid")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOpenOrders(
            @Parameter(description = "User ID") @PathVariable Long userId
    ) {
        List<Map<String, Object>> orders = accountService.getOpenOrders(userId);
        return ResponseEntity.ok(ApiResponse.success("Open orders retrieved successfully", orders));
    }

    @PostMapping("/{userId}/cancel-order")
    @Operation(summary = "Cancel order", description = "Cancel an open order on Hyperliquid")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelOrder(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Asset name (e.g., ETH, BTC)") @RequestParam String asset,
            @Parameter(description = "Order ID to cancel") @RequestParam Long orderId
    ) {
        Map<String, Object> result = accountService.cancelOrder(userId, asset, orderId);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", result));
    }
}
