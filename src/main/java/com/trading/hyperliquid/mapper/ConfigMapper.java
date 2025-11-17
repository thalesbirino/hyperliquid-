package com.trading.hyperliquid.mapper;

import com.trading.hyperliquid.model.dto.request.ConfigRequest;
import com.trading.hyperliquid.model.entity.Config;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Config entity and ConfigRequest DTO.
 * Handles mapping of trading configuration parameters.
 */
@Component
public class ConfigMapper {

    /**
     * Convert ConfigRequest DTO to Config entity.
     * Sets default values from request or fallback to entity defaults.
     *
     * @param request the config creation request
     * @return Config entity ready to be persisted
     */
    public Config toEntity(ConfigRequest request) {
        return Config.builder()
                .name(request.getName())
                .asset(request.getAsset())
                .assetId(request.getAssetId())
                .lotSize(request.getLotSize())
                .slPercent(request.getSlPercent())
                .tpPercent(request.getTpPercent())
                .leverage(request.getLeverage() != null ? request.getLeverage() : 1)
                .orderType(request.getOrderType() != null ? request.getOrderType() : Config.OrderType.LIMIT)
                .timeInForce(request.getTimeInForce() != null ? request.getTimeInForce() : "Gtc")
                .build();
    }

    /**
     * Update existing Config entity with data from ConfigRequest.
     * Preserves existing values for null request fields.
     *
     * @param entity the existing config entity to update
     * @param request the update request
     */
    public void updateEntity(Config entity, ConfigRequest request) {
        entity.setName(request.getName());
        entity.setAsset(request.getAsset());
        entity.setAssetId(request.getAssetId());
        entity.setLotSize(request.getLotSize());
        entity.setSlPercent(request.getSlPercent());
        entity.setTpPercent(request.getTpPercent());

        if (request.getLeverage() != null) {
            entity.setLeverage(request.getLeverage());
        }

        if (request.getOrderType() != null) {
            entity.setOrderType(request.getOrderType());
        }

        if (request.getTimeInForce() != null) {
            entity.setTimeInForce(request.getTimeInForce());
        }
    }
}
