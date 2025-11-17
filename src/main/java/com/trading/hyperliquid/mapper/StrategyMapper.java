package com.trading.hyperliquid.mapper;

import com.trading.hyperliquid.model.dto.request.StrategyRequest;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.model.entity.Strategy;
import com.trading.hyperliquid.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper for converting between Strategy entity and StrategyRequest DTO.
 * Handles password encoding and UUID generation during entity creation.
 */
@Component
@RequiredArgsConstructor
public class StrategyMapper {

    private final PasswordEncoder passwordEncoder;

    /**
     * Convert StrategyRequest DTO to Strategy entity.
     * Generates UUID for strategyId if not provided.
     * Encodes the password using BCrypt.
     *
     * @param request the strategy creation request
     * @param config the associated trading configuration
     * @param user the owner of this strategy
     * @return Strategy entity ready to be persisted
     */
    public Strategy toEntity(StrategyRequest request, Config config, User user) {
        String strategyId = request.getStrategyId();
        if (strategyId == null || strategyId.isEmpty()) {
            strategyId = UUID.randomUUID().toString();
        }

        return Strategy.builder()
                .name(request.getName())
                .strategyId(strategyId)
                .password(passwordEncoder.encode(request.getPassword()))
                .config(config)
                .user(user)
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .inverse(request.getInverse() != null ? request.getInverse() : false)
                .pyramid(request.getPyramid() != null ? request.getPyramid() : false)
                .build();
    }

    /**
     * Update existing Strategy entity with data from StrategyRequest.
     * Only updates password if a new one is provided.
     * Preserves existing values for null request fields.
     *
     * @param entity the existing strategy entity to update
     * @param request the update request
     * @param config the associated trading configuration (if changed)
     * @param user the owner of this strategy (if changed)
     */
    public void updateEntity(Strategy entity, StrategyRequest request, Config config, User user) {
        entity.setName(request.getName());

        if (request.getStrategyId() != null && !request.getStrategyId().isEmpty()) {
            entity.setStrategyId(request.getStrategyId());
        }

        // Only update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            entity.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (config != null) {
            entity.setConfig(config);
        }

        if (user != null) {
            entity.setUser(user);
        }

        entity.setDescription(request.getDescription());

        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }

        if (request.getInverse() != null) {
            entity.setInverse(request.getInverse());
        }

        if (request.getPyramid() != null) {
            entity.setPyramid(request.getPyramid());
        }
    }
}
