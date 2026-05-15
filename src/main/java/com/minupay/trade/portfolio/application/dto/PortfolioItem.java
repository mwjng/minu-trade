package com.minupay.trade.portfolio.application.dto;

import com.minupay.trade.common.money.Money;

import java.math.BigDecimal;

public record PortfolioItem(
        String stockCode,
        String stockName,
        int quantity,
        BigDecimal avgPrice,
        Money totalCost,
        Valuation valuation
) {
}
