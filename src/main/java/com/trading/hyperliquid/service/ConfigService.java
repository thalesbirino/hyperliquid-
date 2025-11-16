package com.trading.hyperliquid.service;

import com.trading.hyperliquid.exception.ResourceNotFoundException;
import com.trading.hyperliquid.model.dto.request.ConfigRequest;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.repository.ConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private final ConfigRepository configRepository;

    public ConfigService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Transactional(readOnly = true)
    public List<Config> getAllConfigs() {
        logger.debug("Fetching all configs");
        return configRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Config getConfigById(Long id) {
        logger.debug("Fetching config with id: {}", id);
        return configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Config not found with id: " + id));
    }

    @Transactional
    public Config createConfig(ConfigRequest request) {
        logger.debug("Creating new config: {}", request.getName());

        Config config = new Config();
        config.setName(request.getName());
        config.setAsset(request.getAsset());
        config.setAssetId(request.getAssetId());
        config.setLotSize(request.getLotSize());
        config.setSlPercent(request.getSlPercent());
        config.setTpPercent(request.getTpPercent());
        config.setLeverage(request.getLeverage() != null ? request.getLeverage() : 1);
        config.setOrderType(request.getOrderType() != null ? request.getOrderType() : Config.OrderType.LIMIT);
        config.setTimeInForce(request.getTimeInForce() != null ? request.getTimeInForce() : "Gtc");

        Config savedConfig = configRepository.save(config);
        logger.info("Created config: {} with id: {}", savedConfig.getName(), savedConfig.getId());

        return savedConfig;
    }

    @Transactional
    public Config updateConfig(Long id, ConfigRequest request) {
        logger.debug("Updating config with id: {}", id);

        Config config = getConfigById(id);

        config.setName(request.getName());
        config.setAsset(request.getAsset());
        config.setAssetId(request.getAssetId());
        config.setLotSize(request.getLotSize());
        config.setSlPercent(request.getSlPercent());
        config.setTpPercent(request.getTpPercent());

        if (request.getLeverage() != null) {
            config.setLeverage(request.getLeverage());
        }

        if (request.getOrderType() != null) {
            config.setOrderType(request.getOrderType());
        }

        if (request.getTimeInForce() != null) {
            config.setTimeInForce(request.getTimeInForce());
        }

        Config updatedConfig = configRepository.save(config);
        logger.info("Updated config: {}", updatedConfig.getName());

        return updatedConfig;
    }

    @Transactional
    public void deleteConfig(Long id) {
        logger.debug("Deleting config with id: {}", id);

        Config config = getConfigById(id);
        configRepository.delete(config);

        logger.info("Deleted config: {}", config.getName());
    }
}
