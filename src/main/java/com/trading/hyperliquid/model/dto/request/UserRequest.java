package com.trading.hyperliquid.model.dto.request;

import com.trading.hyperliquid.model.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Builder.Default
    private User.Role role = User.Role.USER;

    @Pattern(regexp = "^(0x)?[0-9a-fA-F]{64}$", message = "Invalid private key format (must be 64 hex characters)")
    private String hyperliquidPrivateKey;

    @Pattern(regexp = "^0x[0-9a-fA-F]{40}$", message = "Invalid Ethereum address format")
    private String hyperliquidAddress;

    @Pattern(regexp = "^(0x)?[0-9a-fA-F]{64}$", message = "Invalid API wallet private key format (must be 64 hex characters)")
    private String apiWalletPrivateKey; // Optional - API Wallet private key

    @Pattern(regexp = "^0x[0-9a-fA-F]{40}$", message = "Invalid API wallet address format")
    private String apiWalletAddress; // Optional - API Wallet address (used as vaultAddress)

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Boolean isTestnet = true; // true = demo account, false = real account
}
