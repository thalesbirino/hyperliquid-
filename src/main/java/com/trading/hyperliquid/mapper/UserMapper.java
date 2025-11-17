package com.trading.hyperliquid.mapper;

import com.trading.hyperliquid.model.dto.request.UserRequest;
import com.trading.hyperliquid.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between User entity and UserRequest DTO.
 * Handles password encoding during entity creation.
 */
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final PasswordEncoder passwordEncoder;

    /**
     * Convert UserRequest DTO to User entity.
     * Encodes the password using BCrypt.
     * Sets default values from request or fallback to defaults.
     *
     * @param request the user creation/update request
     * @return User entity ready to be persisted
     */
    public User toEntity(UserRequest request) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : User.Role.USER)
                .hyperliquidPrivateKey(request.getHyperliquidPrivateKey())
                .hyperliquidAddress(request.getHyperliquidAddress())
                .active(request.getActive() != null ? request.getActive() : true)
                .isTestnet(request.getIsTestnet() != null ? request.getIsTestnet() : true)
                .build();
    }

    /**
     * Update existing User entity with data from UserRequest.
     * Only updates password if a new one is provided.
     * Preserves existing values for null request fields.
     *
     * @param entity the existing user entity to update
     * @param request the update request
     */
    public void updateEntity(User entity, UserRequest request) {
        entity.setUsername(request.getUsername());
        entity.setEmail(request.getEmail());

        // Only update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            entity.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            entity.setRole(request.getRole());
        }

        entity.setHyperliquidPrivateKey(request.getHyperliquidPrivateKey());
        entity.setHyperliquidAddress(request.getHyperliquidAddress());

        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }

        if (request.getIsTestnet() != null) {
            entity.setIsTestnet(request.getIsTestnet());
        }
    }
}
