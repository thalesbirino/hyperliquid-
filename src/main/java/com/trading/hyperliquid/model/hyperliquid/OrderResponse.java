package com.trading.hyperliquid.model.hyperliquid;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private String status;
    private Response response;
    private String error;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String type;
        private ResponseData data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseData {
        private Statuses statuses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statuses {
        private String filled;
        private String resting;
        private String error;
    }
}
