package com.trading.hyperliquid.service;

import com.trading.hyperliquid.exception.InvalidCredentialsException;
import com.trading.hyperliquid.model.dto.request.LoginRequest;
import com.trading.hyperliquid.model.dto.response.JwtResponse;
import com.trading.hyperliquid.model.entity.User;
import com.trading.hyperliquid.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Service for handling user authentication and JWT token generation.
 * Provides secure login functionality with BCrypt password verification.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Authenticate user credentials and generate JWT access token.
     * Verifies username/password combination using Spring Security's AuthenticationManager
     * and returns a JWT token for subsequent API requests.
     *
     * @param request the login request containing username and password
     * @return JWT response containing token, username, email, and role
     * @throws InvalidCredentialsException if username/password is incorrect or user not found
     */
    public JwtResponse login(LoginRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

            // Generate JWT token
            String token = jwtService.generateToken(userDetails);

            // Get user info
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new InvalidCredentialsException("User not found"));

            logger.info("User {} logged in successfully", request.getUsername());

            return new JwtResponse(
                    token,
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name()
            );

        } catch (BadCredentialsException e) {
            logger.error("Login failed for user: {}", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }
}
