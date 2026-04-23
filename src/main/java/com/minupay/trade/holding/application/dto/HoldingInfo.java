package com.minupay.trade.holding.application.dto;

import com.minupay.trade.holding.domain.Holding;

import java.math.BigDecimal;

public record HoldingInfo(
        Long id,
        Long userId,
        String stockCode,
        int quantity,
        BigDecimal avgPrice
) {
    public static HoldingInfo from(Holding holding) {
        return new HoldingInfo(
                holding.getId(),
                holding.getUserId(),
                holding.getStockCode(),
                holding.getQuantity(),
                holding.getAvgPrice()
        );
    }
}
