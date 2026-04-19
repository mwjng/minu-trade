package com.minupay.trade.order.application.dto;

import com.minupay.trade.order.domain.OrderSide;
import com.minupay.trade.order.domain.OrderType;

import java.math.BigDecimal;

public record PlaceOrderCommand(
        String stockCode,
        OrderSide side,
        OrderType type,
        BigDecimal price,
        int quantity,
        String idempotencyKey
) {
}
