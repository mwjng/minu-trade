package com.minupay.trade.order.domain.orderbook;

import java.math.BigDecimal;

public record Trade(
        long buyOrderId,
        long sellOrderId,
        BigDecimal price,
        int quantity
) {}
