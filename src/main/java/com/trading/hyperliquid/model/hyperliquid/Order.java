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
public class Order {

    private Integer a; // Asset ID (0 for BTC, 1 for ETH, etc.)

    private Boolean b; // Buy flag (true = buy, false = sell)

    private String p; // Price (string format for precision)

    private String s; // Size in base currency (string format)

    private Boolean r; // Reduce-only flag (default false)

    private OrderType t; // Order type (limit, trigger, etc.)

    private String c; // Client order ID (optional, 128-bit hex)

    /**
     * Create a limit buy order
     */
    public static Order limitBuy(int assetId, String price, String size, String tif) {
        return Order.builder()
                .a(assetId)
                .b(true)
                .p(price)
                .s(size)
                .r(false)
                .t(OrderType.limit(tif))
                .build();
    }

    /**
     * Create a limit sell order
     */
    public static Order limitSell(int assetId, String price, String size, String tif) {
        return Order.builder()
                .a(assetId)
                .b(false)
                .p(price)
                .s(size)
                .r(false)
                .t(OrderType.limit(tif))
                .build();
    }
}
