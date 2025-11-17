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

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        logger.debug("Fetching all users");
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        logger.debug("Fetching user with id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

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

    @Transactional
    public void deleteUser(Long id) {
        logger.debug("Deleting user with id: {}", id);

        User user = getUserById(id);
        userRepository.delete(user);

        logger.info("Deleted user: {}", user.getUsername());
    }
}
