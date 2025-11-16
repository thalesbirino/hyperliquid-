package com.trading.hyperliquid.controller;

import com.trading.hyperliquid.model.dto.request.WebhookRequest;
import com.trading.hyperliquid.model.dto.response.WebhookResponse;
import com.trading.hyperliquid.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
@Tag(name = "Webhook", description = "TradingView webhook endpoint for order execution")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    @Operation(
            summary = "Receive TradingView webhook",
            description = "Process webhook from TradingView and execute order on Hyperliquid"
    )
    public ResponseEntity<WebhookResponse> handleWebhook(@Valid @RequestBody WebhookRequest request) {
        logger.info("Received webhook: action={}, strategyId={}", request.getAction(), request.getStrategyId());

        WebhookResponse response = webhookService.processWebhook(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
