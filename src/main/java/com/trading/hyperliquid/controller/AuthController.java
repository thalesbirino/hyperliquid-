package com.trading.hyperliquid.controller;

import com.trading.hyperliquid.model.dto.request.LoginRequest;
import com.trading.hyperliquid.model.dto.response.ApiResponse;
import com.trading.hyperliquid.model.dto.response.JwtResponse;
import com.trading.hyperliquid.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints for login and token generation")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and receive JWT token")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse jwtResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", jwtResponse));
    }
}
