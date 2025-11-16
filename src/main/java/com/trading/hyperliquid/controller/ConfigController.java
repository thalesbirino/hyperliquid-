package com.trading.hyperliquid.controller;

import com.trading.hyperliquid.model.dto.request.ConfigRequest;
import com.trading.hyperliquid.model.dto.response.ApiResponse;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@Tag(name = "Config Management", description = "CRUD operations for trading configurations")
@SecurityRequirement(name = "Bearer Authentication")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    @Operation(summary = "Get all configs", description = "Retrieve list of all trading configurations")
    public ResponseEntity<ApiResponse<List<Config>>> getAllConfigs() {
        List<Config> configs = configService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success("Configs retrieved successfully", configs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get config by ID", description = "Retrieve a specific config by its ID")
    public ResponseEntity<ApiResponse<Config>> getConfigById(@PathVariable Long id) {
        Config config = configService.getConfigById(id);
        return ResponseEntity.ok(ApiResponse.success("Config retrieved successfully", config));
    }

    @PostMapping
    @Operation(summary = "Create new config", description = "Create a new trading configuration")
    public ResponseEntity<ApiResponse<Config>> createConfig(@Valid @RequestBody ConfigRequest request) {
        Config config = configService.createConfig(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Config created successfully", config));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update config", description = "Update an existing trading configuration")
    public ResponseEntity<ApiResponse<Config>> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody ConfigRequest request
    ) {
        Config config = configService.updateConfig(id, request);
        return ResponseEntity.ok(ApiResponse.success("Config updated successfully", config));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete config", description = "Delete a trading configuration")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable Long id) {
        configService.deleteConfig(id);
        return ResponseEntity.ok(ApiResponse.success("Config deleted successfully", null));
    }
}
