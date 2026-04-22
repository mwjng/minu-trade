package com.minupay.trade.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByAccountIdOrderByIdDesc(Long accountId);
}
