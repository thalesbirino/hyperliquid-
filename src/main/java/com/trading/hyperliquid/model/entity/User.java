package com.trading.hyperliquid.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password", "hyperliquidPrivateKey", "apiWalletPrivateKey"})
@EqualsAndHashCode(exclude = {"password", "hyperliquidPrivateKey", "apiWalletPrivateKey"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password; // BCrypt hashed password for admin login

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @JsonIgnore
    @Column(name = "hyperliquid_private_key", length = 66)
    private String hyperliquidPrivateKey; // Wallet private key for signing

    @Column(name = "hyperliquid_address", length = 42)
    private String hyperliquidAddress; // Wallet address (0x...)

    @JsonIgnore
    @Column(name = "api_wallet_private_key", length = 66)
    private String apiWalletPrivateKey; // API Wallet private key for signing (optional)

    @Column(name = "api_wallet_address", length = 42)
    private String apiWalletAddress; // API Wallet address (0x...) - used as vaultAddress when present

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "is_testnet", nullable = false)
    @Builder.Default
    private Boolean isTestnet = true; // true = demo account, false = real account

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Role {
        ADMIN,
        USER
    }
}
