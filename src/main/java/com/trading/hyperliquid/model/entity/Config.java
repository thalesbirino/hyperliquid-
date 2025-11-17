package com.trading.hyperliquid.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String asset; // e.g., "ETH", "BTC", "SOL"

    @Column(name = "asset_id", nullable = false)
    private Integer assetId; // Hyperliquid asset ID (0 for BTC, 1 for ETH, etc.)

    @Column(name = "lot_size", nullable = false, precision = 18, scale = 8)
    private BigDecimal lotSize; // Order size in base currency

    @Column(name = "sl_percent", precision = 5, scale = 2)
    private BigDecimal slPercent; // Stop-loss percentage (e.g., 2.00 for 2%)

    @Column(name = "tp_percent", precision = 5, scale = 2)
    private BigDecimal tpPercent; // Take-profit percentage (optional)

    @Column(nullable = false)
    @Builder.Default
    private Integer leverage = 1; // Leverage (1-50)

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    @Builder.Default
    private OrderType orderType = OrderType.LIMIT;

    @Column(name = "time_in_force", length = 10)
    @Builder.Default
    private String timeInForce = "Gtc"; // Gtc, Ioc, Alo

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum OrderType {
        MARKET,
        LIMIT
    }
}
