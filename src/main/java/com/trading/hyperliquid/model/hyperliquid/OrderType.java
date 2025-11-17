package com.trading.hyperliquid.model.hyperliquid;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderType {

    private LimitOrderType limit;
    private TriggerOrderType trigger;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimitOrderType {
        private String tif = "Gtc"; // Time in force: Gtc, Ioc, Alo
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TriggerOrderType {
        private boolean isMarket;
        private String triggerPx; // Trigger price
        private String tpsl; // "tp" or "sl"
    }

    public static OrderType limit(String tif) {
        OrderType orderType = new OrderType();
        orderType.setLimit(new LimitOrderType(tif));
        return orderType;
    }

    /**
     * Create trigger order type for stop-loss or take-profit
     *
     * @param triggerPrice Price that activates the order
     * @param tpsl "tp" for take-profit, "sl" for stop-loss
     * @param isMarket true to execute as market order when triggered
     * @return OrderType with trigger configuration
     */
    public static OrderType trigger(String triggerPrice, String tpsl, boolean isMarket) {
        OrderType orderType = new OrderType();
        TriggerOrderType trigger = new TriggerOrderType();
        trigger.setMarket(isMarket);
        trigger.setTriggerPx(triggerPrice);
        trigger.setTpsl(tpsl);
        orderType.setTrigger(trigger);
        return orderType;
    }
}
