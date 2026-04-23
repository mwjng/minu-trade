package com.minupay.trade.holding.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    Optional<Holding> findByUserIdAndStockCode(Long userId, String stockCode);

    List<Holding> findAllByUserIdOrderByStockCodeAsc(Long userId);
}
