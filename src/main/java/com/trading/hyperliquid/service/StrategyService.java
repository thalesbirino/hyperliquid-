package com.trading.hyperliquid.service;

import com.trading.hyperliquid.exception.InvalidStrategyException;
import com.trading.hyperliquid.exception.ResourceNotFoundException;
import com.trading.hyperliquid.model.dto.request.StrategyRequest;
import com.trading.hyperliquid.model.entity.Config;
import com.trading.hyperliquid.model.entity.Strategy;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.repository.StrategyRepository;
import com.trading.hyperliquid.service.base.BaseService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing trading strategy entities.
 * Strategies link TradingView webhooks to specific users and trading configurations.
 * Each strategy has a unique ID and password for webhook authentication.
 * Extends BaseService for common CRUD operations.
 */
@Service
public class StrategyService extends BaseService<Strategy, Long, StrategyRepository> {

    private final ConfigService configService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public StrategyService(
            StrategyRepository strategyRepository,
            ConfigService configService,
            UserService userService,
            PasswordEncoder passwordEncoder
    ) {
        super(strategyRepository, "Strategy");
        this.configService = configService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieve all trading strategies.
     * Delegates to base class findAll() method.
     *
     * @return list of all strategies
     */
    @Transactional(readOnly = true)
    public List<Strategy> getAllStrategies() {
        return findAll();
    }

    /**
     * Retrieve a specific trading strategy by database ID.
     * Delegates to base class findById() method.
     *
     * @param id the strategy database ID
     * @return the strategy entity
     * @throws com.trading.hyperliquid.exception.ResourceNotFoundException if strategy with given ID not found
     */
    @Transactional(readOnly = true)
    public Strategy getStrategyById(Long id) {
        return findById(id);
    }

    /**
     * Retrieve a specific trading strategy by TradingView strategy ID (UUID).
     *
     * @param strategyId the TradingView strategy ID
     * @return the strategy entity
     * @throws ResourceNotFoundException if strategy with given strategyId not found
     */
    @Transactional(readOnly = true)
    public Strategy getStrategyByStrategyId(String strategyId) {
        logger.debug("Fetching strategy with strategyId: {}", strategyId);
        return repository.findByStrategyId(strategyId)
                .orElseThrow(() -> new ResourceNotFoundException("Strategy not found with strategyId: " + strategyId));
    }

    /**
     * Create a new trading strategy.
     * Generates a UUID for strategyId if not provided.
     * Encrypts the password using BCrypt.
     * Sets default values for active (true), inverse (false), and pyramid (false).
     *
     * @param request the strategy creation request
     * @return the created strategy entity
     * @throws IllegalArgumentException if strategyId already exists
     * @throws ResourceNotFoundException if config or user not found
     */
    @Transactional
    public Strategy createStrategy(StrategyRequest request) {
        logger.debug("Creating new strategy: {}", request.getName());

        // Generate strategyId if not provided
        String strategyId = request.getStrategyId();
        if (strategyId == null || strategyId.isEmpty()) {
            strategyId = UUID.randomUUID().toString();
        }

        // Check if strategyId already exists
        if (repository.existsByStrategyId(strategyId)) {
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
        strategy.setInverse(request.getInverse() != null ? request.getInverse() : false);
        strategy.setPyramid(request.getPyramid() != null ? request.getPyramid() : false);

        Strategy savedStrategy = save(strategy);
        logger.info("Created strategy: {} with id: {} and strategyId: {}",
                savedStrategy.getName(), savedStrategy.getId(), savedStrategy.getStrategyId());

        return savedStrategy;
    }

    /**
     * Update an existing trading strategy.
     * Only updates fields that are non-null in the request.
     * Password is only updated if a new password is provided.
     *
     * @param id the strategy database ID to update
     * @param request the strategy update request
     * @return the updated strategy entity
     * @throws IllegalArgumentException if new strategyId already exists
     * @throws ResourceNotFoundException if strategy, config, or user not found
     */
    @Transactional
    public Strategy updateStrategy(Long id, StrategyRequest request) {
        logger.debug("Updating strategy with id: {}", id);

        Strategy strategy = getStrategyById(id);

        // Check strategyId uniqueness if changed
        if (request.getStrategyId() != null &&
                !strategy.getStrategyId().equals(request.getStrategyId()) &&
                repository.existsByStrategyId(request.getStrategyId())) {
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

        if (request.getInverse() != null) {
            strategy.setInverse(request.getInverse());
        }

        if (request.getPyramid() != null) {
            strategy.setPyramid(request.getPyramid());
        }

        Strategy updatedStrategy = save(strategy);
        logger.info("Updated strategy: {}", updatedStrategy.getName());

        return updatedStrategy;
    }

    /**
     * Delete a trading strategy.
     * Delegates to base class deleteById() method.
     *
     * @param id the strategy database ID to delete
     * @throws com.trading.hyperliquid.exception.ResourceNotFoundException if strategy with given ID not found
     */
    @Transactional
    public void deleteStrategy(Long id) {
        deleteById(id);
    }

    /**
     * Validate strategy credentials for webhook authentication.
     * Verifies that the strategyId exists, the strategy is active,
     * and the password matches the stored BCrypt hash.
     *
     * @param strategyId the TradingView strategy ID
     * @param password the plaintext password to verify
     * @return the validated strategy entity
     * @throws InvalidStrategyException if strategyId not found, strategy inactive, or password incorrect
     */
    @Transactional(readOnly = true)
    public Strategy validateStrategyCredentials(String strategyId, String password) {
        logger.debug("Validating credentials for strategyId: {}", strategyId);

        Strategy strategy = repository.findByStrategyId(strategyId)
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
