package com.trading.hyperliquid.service;

import com.trading.hyperliquid.exception.ResourceNotFoundException;
import com.trading.hyperliquid.model.dto.request.UserRequest;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing user entities.
 * Handles CRUD operations for users who connect trading strategies to Hyperliquid Exchange.
 * Stores Hyperliquid wallet credentials and manages testnet/mainnet routing.
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieve all users.
     *
     * @return list of all users
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        logger.debug("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Retrieve a specific user by ID.
     *
     * @param id the user ID
     * @return the user entity
     * @throws ResourceNotFoundException if user with given ID not found
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        logger.debug("Fetching user with id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
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

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : User.Role.USER);
        user.setHyperliquidPrivateKey(request.getHyperliquidPrivateKey());
        user.setHyperliquidAddress(request.getHyperliquidAddress());
        user.setActive(request.getActive() != null ? request.getActive() : true);
        user.setIsTestnet(request.getIsTestnet() != null ? request.getIsTestnet() : true);

        User savedUser = userRepository.save(user);
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
                userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Check email uniqueness if changed
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        // Only update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        user.setHyperliquidPrivateKey(request.getHyperliquidPrivateKey());
        user.setHyperliquidAddress(request.getHyperliquidAddress());

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        if (request.getIsTestnet() != null) {
            user.setIsTestnet(request.getIsTestnet());
        }

        User updatedUser = userRepository.save(user);
        logger.info("Updated user: {}", updatedUser.getUsername());

        return updatedUser;
    }

    /**
     * Delete a user.
     *
     * @param id the user ID to delete
     * @throws ResourceNotFoundException if user with given ID not found
     */
    @Transactional
    public void deleteUser(Long id) {
        logger.debug("Deleting user with id: {}", id);

        User user = getUserById(id);
        userRepository.delete(user);

        logger.info("Deleted user: {}", user.getUsername());
    }
}
