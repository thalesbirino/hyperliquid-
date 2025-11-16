package com.trading.hyperliquid.model.hyperliquid;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderAction {

    private String type; // "order", "cancel", "cancelByCloid", "modify", etc.

    private List<Order> orders; // For order placement

    private String grouping; // "na" for normal, or grouping strategy

    private List<CancelRequest> cancels; // For cancel operations

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelRequest {
        private Integer a; // Asset ID
        private Long o; // Order ID
        private String c; // Client order ID (for cancelByCloid)
    }

    /**
     * Create order action for placing orders
     */
    public static OrderAction placeOrder(List<Order> orders) {
        return OrderAction.builder()
                .type("order")
                .orders(orders)
                .grouping("na")
                .build();
    }

    /**
     * Create order action for placing a single order
     */
    public static OrderAction placeOrder(Order order) {
        return placeOrder(List.of(order));
    }
}
