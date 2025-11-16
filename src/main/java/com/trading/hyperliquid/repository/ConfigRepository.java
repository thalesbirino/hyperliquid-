package com.trading.hyperliquid.repository;

import com.trading.hyperliquid.model.entity.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {

    Optional<Config> findByAsset(String asset);

    List<Config> findByAssetId(Integer assetId);
}
