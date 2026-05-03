package com.minupay.trade.marketdata.application.dto;

import com.minupay.trade.marketdata.domain.PriceLevel;
import com.minupay.trade.marketdata.domain.Quote;

import java.time.Instant;
import java.util.List;

public record QuoteSnapshot(
        String stockCode,
        long currentPrice,
        long openPrice,
        long highPrice,
        long lowPrice,
        double changeRate,
        long volume,
        List<PriceLevel> askPrices,
        List<PriceLevel> bidPrices,
        Instant occurredAt
) {
    public Quote toQuote() {
        return Quote.of(stockCode, currentPrice, openPrice, highPrice, lowPrice,
                changeRate, volume, askPrices, bidPrices, occurredAt);
    }
}
