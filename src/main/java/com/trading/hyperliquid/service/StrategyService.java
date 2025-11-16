package com.trading.hyperliquid.service;

import com.trading.hyperliquid.exception.InvalidStrategyException;
import com.trading.hyperliquid.exception.ResourceNotFoundException;
import com.trading.hyperliquid.model.dto.request.StrategyRequest;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.model.entity.Strategy;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.repository.StrategyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StrategyService {

    private static final Logger logger = LoggerFactory.getLogger(StrategyService.class);

    private final StrategyRepository strategyRepository;
    private final ConfigService configService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public StrategyService(
            StrategyRepository strategyRepository,
            ConfigService configService,
            UserService userService,
            PasswordEncoder passwordEncoder
    ) {
        this.strategyRepository = strategyRepository;
        this.configService = configService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Strategy> getAllStrategies() {
        logger.debug("Fetching all strategies");
        return strategyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Strategy getStrategyById(Long id) {
        logger.debug("Fetching strategy with id: {}", id);
        return strategyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Strategy not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Strategy getStrategyByStrategyId(String strategyId) {
        logger.debug("Fetching strategy with strategyId: {}", strategyId);
        return strategyRepository.findByStrategyId(strategyId)
                .orElseThrow(() -> new ResourceNotFoundException("Strategy not found with strategyId: " + strategyId));
    }

    @Transactional
    public Strategy createStrategy(StrategyRequest request) {
        logger.debug("Creating new strategy: {}", request.getName());

        // Generate strategyId if not provided
        String strategyId = request.getStrategyId();
        if (strategyId == null || strategyId.isEmpty()) {
            strategyId = UUID.randomUUID().toString();
        }

        // Check if strategyId already exists
        if (strategyRepository.existsByStrategyId(strategyId)) {
            throw new IllegalArgumentException("Strategy ID already exists: " + strategyId);
        }

        // Fetch related entities
        Config config = configService.getConfigById(request.getConfigId());
        User user = userService.getUserById(request.getUserId());

        // Create strategy
        Strategy strategy = new Strategy();
        strategy.setName(request.getName());
        strategy.setStrategyId(strategyId);
        strategy.setPassword(passwordEncoder.encode(request.getPassword()));
        strategy.setConfig(config);
        strategy.setUser(user);
        strategy.setDescription(request.getDescription());
        strategy.setActive(request.getActive() != null ? request.getActive() : true);

        Strategy savedStrategy = strategyRepository.save(strategy);
        logger.info("Created strategy: {} with id: {} and strategyId: {}",
                savedStrategy.getName(), savedStrategy.getId(), savedStrategy.getStrategyId());

        return savedStrategy;
    }

    @Transactional
    public Strategy updateStrategy(Long id, StrategyRequest request) {
        logger.debug("Updating strategy with id: {}", id);

        Strategy strategy = getStrategyById(id);

        // Check strategyId uniqueness if changed
        if (request.getStrategyId() != null &&
                !strategy.getStrategyId().equals(request.getStrategyId()) &&
                strategyRepository.existsByStrategyId(request.getStrategyId())) {
            throw new IllegalArgumentException("Strategy ID already exists: " + request.getStrategyId());
        }

        // Update fields
        strategy.setName(request.getName());

        if (request.getStrategyId() != null && !request.getStrategyId().isEmpty()) {
            strategy.setStrategyId(request.getStrategyId());
        }

        // Only update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            strategy.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getConfigId() != null) {
            Config config = configService.getConfigById(request.getConfigId());
            strategy.setConfig(config);
        }

        if (request.getUserId() != null) {
            User user = userService.getUserById(request.getUserId());
            strategy.setUser(user);
        }

        strategy.setDescription(request.getDescription());

        if (request.getActive() != null) {
            strategy.setActive(request.getActive());
        }

        Strategy updatedStrategy = strategyRepository.save(strategy);
        logger.info("Updated strategy: {}", updatedStrategy.getName());

        return updatedStrategy;
    }

    @Transactional
    public void deleteStrategy(Long id) {
        logger.debug("Deleting strategy with id: {}", id);

        Strategy strategy = getStrategyById(id);
        strategyRepository.delete(strategy);

        logger.info("Deleted strategy: {}", strategy.getName());
    }

    /**
     * Validate strategy credentials (for webhook)
     */
    @Transactional(readOnly = true)
    public Strategy validateStrategyCredentials(String strategyId, String password) {
        logger.debug("Validating credentials for strategyId: {}", strategyId);

        Strategy strategy = strategyRepository.findByStrategyId(strategyId)
                .orElseThrow(() -> new InvalidStrategyException("Invalid strategy ID or password"));

        if (!strategy.getActive()) {
            throw new InvalidStrategyException("Strategy is inactive");
        }

        if (!passwordEncoder.matches(password, strategy.getPassword())) {
            logger.warn("Invalid password attempt for strategy: {}", strategyId);
            throw new InvalidStrategyException("Invalid strategy ID or password");
        }

        logger.info("Strategy credentials validated successfully for: {}", strategyId);
        return strategy;
    }
}
