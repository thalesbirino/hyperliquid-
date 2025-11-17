package com.trading.hyperliquid.service;

import com.trading.hyperliquid.mapper.ConfigMapper;
import com.trading.hyperliquid.model.dto.request.ConfigRequest;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.repository.ConfigRepository;
import com.trading.hyperliquid.service.base.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing trading configuration entities.
 * Handles CRUD operations for Config objects which define trading parameters
 * such as asset, lot size, leverage, stop-loss, and take-profit percentages.
 * Extends BaseService for common CRUD operations.
 */
@Service
public class ConfigService extends BaseService<Config, Long, ConfigRepository> {

    private final ConfigMapper configMapper;

    public ConfigService(ConfigRepository configRepository, ConfigMapper configMapper) {
        super(configRepository, "Config");
        this.configMapper = configMapper;
    }

    /**
     * Retrieve all trading configurations.
     * Delegates to base class findAll() method.
     *
     * @return list of all configs
     */
    @Transactional(readOnly = true)
    public List<Config> getAllConfigs() {
        return findAll();
    }

    /**
     * Retrieve a specific trading configuration by ID.
     * Delegates to base class findById() method.
     *
     * @param id the config ID
     * @return the config entity
     * @throws com.trading.hyperliquid.exception.ResourceNotFoundException if config with given ID not found
     */
    @Transactional(readOnly = true)
    public Config getConfigById(Long id) {
        return findById(id);
    }

    /**
     * Create a new trading configuration.
     * Sets default values for leverage (1), order type (LIMIT), and time-in-force (Gtc)
     * if not provided in the request.
     *
     * @param request the config creation request
     * @return the created config entity
     */
    @Transactional
    public Config createConfig(ConfigRequest request) {
        logger.debug("Creating new config: {}", request.getName());

        Config config = configMapper.toEntity(request);
        Config savedConfig = save(config);
        logger.info("Created config: {} with id: {}", savedConfig.getName(), savedConfig.getId());

        return savedConfig;
    }

    /**
     * Update an existing trading configuration.
     * Only updates fields that are non-null in the request.
     *
     * @param id the config ID to update
     * @param request the config update request
     * @return the updated config entity
     * @throws ResourceNotFoundException if config with given ID not found
     */
    @Transactional
    public Config updateConfig(Long id, ConfigRequest request) {
        logger.debug("Updating config with id: {}", id);

        Config config = getConfigById(id);
        configMapper.updateEntity(config, request);
        Config updatedConfig = save(config);
        logger.info("Updated config: {}", updatedConfig.getName());

        return updatedConfig;
    }

    /**
     * Delete a trading configuration.
     * Delegates to base class deleteById() method.
     *
     * @param id the config ID to delete
     * @throws com.trading.hyperliquid.exception.ResourceNotFoundException if config with given ID not found
     */
    @Transactional
    public void deleteConfig(Long id) {
        deleteById(id);
    }
}
