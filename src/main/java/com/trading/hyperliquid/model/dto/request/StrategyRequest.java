package com.trading.hyperliquid.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyRequest {

    @NotBlank(message = "Strategy name is required")
    @Size(max = 200, message = "Strategy name must not exceed 200 characters")
    private String name;

    private String strategyId; // Optional - will be generated if not provided

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotNull(message = "Config ID is required")
    private Long configId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Boolean inverse = false;

    @Builder.Default
    private Boolean pyramid = false;
}
