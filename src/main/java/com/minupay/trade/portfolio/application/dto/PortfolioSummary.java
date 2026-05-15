package com.minupay.trade.portfolio.application.dto;

import com.minupay.trade.common.money.Money;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioSummary(
        Money totalCost,
        Money totalMarketValue,
        BigDecimal totalUnrealizedPnl,
        BigDecimal totalReturnRate,
        List<PortfolioItem> items
) {
}
