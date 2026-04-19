package com.minupay.trade.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {
    List<Execution> findByBuyOrderIdOrSellOrderIdOrderByIdAsc(Long buyOrderId, Long sellOrderId);
}
