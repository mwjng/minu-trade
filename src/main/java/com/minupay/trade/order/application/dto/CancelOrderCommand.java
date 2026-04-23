package com.minupay.trade.order.application.dto;

public record CancelOrderCommand(
        Long orderId,
        String stockCode,
        Long userId
) {}
