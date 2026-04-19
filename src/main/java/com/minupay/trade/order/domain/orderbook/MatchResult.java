package com.minupay.trade.order.domain.orderbook;

import java.util.List;

public record MatchResult(
        List<Trade> trades,
        int remainingQuantity,
        boolean restingEntryAdded
) {}
