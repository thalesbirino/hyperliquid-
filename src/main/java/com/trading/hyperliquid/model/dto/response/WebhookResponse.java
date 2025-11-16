package com.trading.hyperliquid.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookResponse {

    private boolean success;
    private String message;
    private String orderId;
    private String action;
    private String asset;
    private String size;
    private String price;
    private String status;
    private LocalDateTime executedAt;

    public static WebhookResponse success(String orderId, String action, String asset,
                                         String size, String price, String status) {
        return new WebhookResponse(
            true,
            "Order executed successfully",
            orderId,
            action,
            asset,
            size,
            price,
            status,
            LocalDateTime.now()
        );
    }

    public static WebhookResponse error(String message) {
        WebhookResponse response = new WebhookResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setExecutedAt(LocalDateTime.now());
        return response;
    }
}
