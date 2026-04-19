package com.minupay.trade.order.application.dto;

import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderSide;
import com.minupay.trade.order.domain.OrderStatus;
import com.minupay.trade.order.domain.OrderType;

import java.math.BigDecimal;

public record OrderInfo(
        Long id,
        Long accountId,
        String stockCode,
        OrderSide side,
        OrderType type,
        BigDecimal price,
        int quantity,
        int filledQuantity,
        OrderStatus status,
        Long paymentId
) {
    public static OrderInfo from(Order order) {
        return new OrderInfo(
                order.getId(),
                order.getAccountId(),
                order.getStockCode(),
                order.getSide(),
                order.getType(),
                order.getPrice(),
                order.getQuantity(),
                order.getFilledQuantity(),
                order.getStatus(),
                order.getPaymentId()
        );
    }
}
