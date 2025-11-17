package com.trading.hyperliquid.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "strategies", indexes = {
    @Index(name = "idx_strategy_id", columnList = "strategy_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
@EqualsAndHashCode(exclude = "password")
public class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "strategy_id", nullable = false, unique = true, length = 36)
    private String strategyId; // UUID from TradingView

    @JsonIgnore
    @Column(nullable = false)
    private String password; // BCrypt hashed password

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "config_id", nullable = false)
    private Config config;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean inverse = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pyramid = false;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (strategyId == null || strategyId.isEmpty()) {
            strategyId = UUID.randomUUID().toString();
        }
    }
}
