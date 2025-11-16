package com.trading.hyperliquid.repository;

import com.trading.hyperliquid.model.entity.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StrategyRepository extends JpaRepository<Strategy, Long> {

    Optional<Strategy> findByStrategyId(String strategyId);

    List<Strategy> findByUserId(Long userId);

    List<Strategy> findByActive(Boolean active);

    boolean existsByStrategyId(String strategyId);
}
