package com.trading.hyperliquid.service;

import com.trading.hyperliquid.mapper.UserMapper;
import com.trading.hyperliquid.model.dto.request.UserRequest;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.repository.UserRepository;
import com.trading.hyperliquid.service.base.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing user entities.
 * Handles CRUD operations for users who connect trading strategies to Hyperliquid Exchange.
 * Stores Hyperliquid wallet credentials and manages testnet/mainnet routing.
 * Extends BaseService for common CRUD operations.
 */
@Service
public class UserService extends BaseService<User, Long, UserRepository> {

    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        super(userRepository, "User");
        this.userMapper = userMapper;
    }

    /**
     * Retrieve all users.
     * Delegates to base class findAll() method.
     *
     * @return list of all users
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return findAll();
    }

    /**
     * Retrieve a specific user by ID.
     * Delegates to base class findById() method.
     *
     * @param id the user ID
     * @return the user entity
     * @throws com.trading.hyperliquid.exception.ResourceNotFoundException if user with given ID not found
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return findById(id);
    }

    /**
     * Create a new user.
     * Encrypts the password using BCrypt.
     * Sets default values for role (USER), active (true), and isTestnet (true).
     *
     * @param request the user creation request
     * @return the created user entity
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public User createUser(UserRequest request) {
        logger.debug("Creating new user: {}", request.getUsername());

        if (repository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        if (repository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        User savedUser = save(user);
        logger.info("Created user: {} with id: {}", savedUser.getUsername(), savedUser.getId());

        return savedUser;
    }

    /**
     * Update an existing user.
     * Only updates fields that are non-null in the request.
     * Password is only updated if a new password is provided.
     *
     * @param id the user ID to update
     * @param request the user update request
     * @return the updated user entity
     * @throws IllegalArgumentException if new username or email already exists
     * @throws ResourceNotFoundException if user with given ID not found
     */
    @Transactional
    public User updateUser(Long id, UserRequest request) {
        logger.debug("Updating user with id: {}", id);

        User user = getUserById(id);

        // Check username uniqueness if changed
        if (!user.getUsername().equals(request.getUsername()) &&
                repository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Check email uniqueness if changed
        if (!user.getEmail().equals(request.getEmail()) &&
                repository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        userMapper.updateEntity(user, request);
        User updatedUser = save(user);
        logger.info("Updated user: {}", updatedUser.getUsername());

        return updatedUser;
    }

    /**
     * Delete a user.
     * Delegates to base class deleteById() method.
     *
     * @param id the user ID to delete
     * @throws com.trading.hyperliquid.exception.ResourceNotFoundException if user with given ID not found
     */
    @Transactional
    public void deleteUser(Long id) {
        deleteById(id);
    }
}
