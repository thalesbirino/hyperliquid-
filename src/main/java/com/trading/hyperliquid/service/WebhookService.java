package com.trading.hyperliquid.service;

import com.trading.hyperliquid.model.dto.request.WebhookRequest;
import com.trading.hyperliquid.model.dto.response.WebhookResponse;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.model.entity.Strategy;
import com.trading.hyperliquid.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final StrategyService strategyService;
    private final HyperliquidService hyperliquidService;

    /**
     * Process TradingView webhook and execute order
     */
    @Transactional
    public WebhookResponse processWebhook(WebhookRequest request) {
        log.info("Processing webhook - Action: {}, StrategyID: {}", request.getAction(), request.getStrategyId());

        try {
            // 1. Validate strategy credentials
            Strategy strategy = strategyService.validateStrategyCredentials(
                    request.getStrategyId(),
                    request.getPassword()
            );

            // 2. Get associated config and user
            Config config = strategy.getConfig();
            User user = strategy.getUser();

            log.debug("Strategy '{}' validated. Asset: {}, User: {}",
                    strategy.getName(), config.getAsset(), user.getUsername());

            // 3. Execute order on Hyperliquid
            String orderId = hyperliquidService.placeOrder(request.getAction(), config, user);

            // 4. Build and return success response
            return WebhookResponse.success(
                    orderId,
                    request.getAction().toUpperCase(),
                    config.getAsset(),
                    config.getLotSize().toPlainString(),
                    "MARKET", // Price determined by Hyperliquid service
                    "EXECUTED"
            );

        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage(), e);
            return WebhookResponse.error("Order execution failed: " + e.getMessage());
        }
    }
}
