package com.trading.hyperliquid.model.hyperliquid;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HyperliquidRequest {

    private OrderAction action;

    private Long nonce;

    private String signature;

    private String vaultAddress; // Optional, for vault operations

    private Long expiresAfter; // Optional, expiration timestamp
}
