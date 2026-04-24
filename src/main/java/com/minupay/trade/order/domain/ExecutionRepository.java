package com.minupay.trade.order.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {
    List<Execution> findByBuyOrderIdOrSellOrderIdOrderByIdAsc(Long buyOrderId, Long sellOrderId);

    @Query("select e from Execution e where e.buyOrderId = :orderId or e.sellOrderId = :orderId")
    Page<Execution> findByOrderId(@Param("orderId") Long orderId, Pageable pageable);
}
