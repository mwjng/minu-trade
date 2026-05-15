package com.minupay.trade.portfolio.application.dto;

import com.minupay.trade.common.money.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Valuation(
        long currentPrice,
        Money marketValue,
        BigDecimal unrealizedPnl,
        BigDecimal returnRate
) {
    private static final int RETURN_RATE_SCALE = 4;

    public static Valuation of(long currentPrice, int quantity, Money totalCost) {
        Money marketValue = Money.of(currentPrice).multiply(quantity);
        BigDecimal pnl = marketValue.getAmount().subtract(totalCost.getAmount());
        return new Valuation(currentPrice, marketValue, pnl, returnRate(pnl, totalCost.getAmount()));
    }

    public static BigDecimal returnRate(BigDecimal pnl, BigDecimal cost) {
        if (cost.signum() == 0) {
            return BigDecimal.ZERO.setScale(RETURN_RATE_SCALE, RoundingMode.HALF_UP);
        }
        return pnl.divide(cost, RETURN_RATE_SCALE, RoundingMode.HALF_UP);
    }
}
