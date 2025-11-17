package com.trading.hyperliquid.controller;

import com.trading.hyperliquid.model.dto.request.StrategyRequest;
import com.trading.hyperliquid.model.dto.response.ApiResponse;
import com.trading.hyperliquid.model.entity.Strategy;
import com.trading.hyperliquid.service.StrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/strategy")
@RequiredArgsConstructor
@Tag(name = "Strategy Management", description = "CRUD operations for trading strategies")
@SecurityRequirement(name = "Bearer Authentication")
public class StrategyController {

    private final StrategyService strategyService;

    @GetMapping
    @Operation(summary = "Get all strategies", description = "Retrieve list of all trading strategies")
    public ResponseEntity<ApiResponse<List<Strategy>>> getAllStrategies() {
        List<Strategy> strategies = strategyService.getAllStrategies();
        return ResponseEntity.ok(ApiResponse.success("Strategies retrieved successfully", strategies));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get strategy by ID", description = "Retrieve a specific strategy by its ID")
    public ResponseEntity<ApiResponse<Strategy>> getStrategyById(@PathVariable Long id) {
        Strategy strategy = strategyService.getStrategyById(id);
        return ResponseEntity.ok(ApiResponse.success("Strategy retrieved successfully", strategy));
    }

    @PostMapping
    @Operation(summary = "Create new strategy", description = "Create a new trading strategy")
    public ResponseEntity<ApiResponse<Strategy>> createStrategy(@Valid @RequestBody StrategyRequest request) {
        Strategy strategy = strategyService.createStrategy(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Strategy created successfully", strategy));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update strategy", description = "Update an existing trading strategy")
    public ResponseEntity<ApiResponse<Strategy>> updateStrategy(
            @PathVariable Long id,
            @Valid @RequestBody StrategyRequest request
    ) {
        Strategy strategy = strategyService.updateStrategy(id, request);
        return ResponseEntity.ok(ApiResponse.success("Strategy updated successfully", strategy));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete strategy", description = "Delete a trading strategy")
    public ResponseEntity<ApiResponse<Void>> deleteStrategy(@PathVariable Long id) {
        strategyService.deleteStrategy(id);
        return ResponseEntity.ok(ApiResponse.success("Strategy deleted successfully", null));
    }
}
