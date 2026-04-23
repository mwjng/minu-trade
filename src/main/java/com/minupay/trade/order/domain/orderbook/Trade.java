package com.minupay.trade.order.domain.orderbook;

import com.minupay.trade.common.money.Money;

public record Trade(
        long buyOrderId,
        long sellOrderId,
        Money price,
        int quantity
) {}
