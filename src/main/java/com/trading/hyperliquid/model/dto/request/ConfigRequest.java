package com.trading.hyperliquid.model.dto.request;

import com.trading.hyperliquid.model.entity.Config;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigRequest {

    @NotBlank(message = "Config name is required")
    @Size(max = 100, message = "Config name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Asset is required")
    @Size(max = 20, message = "Asset must not exceed 20 characters")
    private String asset;

    @NotNull(message = "Asset ID is required")
    @Min(value = 0, message = "Asset ID must be non-negative")
    private Integer assetId;

    @NotNull(message = "Lot size is required")
    @DecimalMin(value = "0.00000001", message = "Lot size must be positive")
    private BigDecimal lotSize;

    @DecimalMin(value = "0.01", message = "Stop-loss percent must be at least 0.01")
    @DecimalMax(value = "100.00", message = "Stop-loss percent must not exceed 100")
    private BigDecimal slPercent;

    @DecimalMin(value = "0.01", message = "Take-profit percent must be at least 0.01")
    @DecimalMax(value = "1000.00", message = "Take-profit percent must not exceed 1000")
    private BigDecimal tpPercent;

    @Min(value = 1, message = "Leverage must be at least 1")
    @Max(value = 50, message = "Leverage must not exceed 50")
    @Builder.Default
    private Integer leverage = 1;

    @Builder.Default
    private Config.OrderType orderType = Config.OrderType.LIMIT;

    @Pattern(regexp = "^(Gtc|Ioc|Alo)$", message = "Time in force must be Gtc, Ioc, or Alo")
    @Builder.Default
    private String timeInForce = "Gtc";
}
