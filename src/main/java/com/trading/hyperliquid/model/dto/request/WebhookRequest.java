package com.trading.hyperliquid.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRequest {

    @NotBlank(message = "Action is required")
    @Pattern(regexp = "(?i)^(buy|sell)$", message = "Action must be either 'buy' or 'sell'")
    private String action;

    @NotBlank(message = "Strategy ID is required")
    private String strategyId;

    @NotBlank(message = "Password is required")
    private String password;
}
