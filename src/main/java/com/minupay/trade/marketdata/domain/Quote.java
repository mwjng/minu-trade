package com.minupay.trade.marketdata.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "quotes")
public record Quote(
        @Id String stockCode,
        long currentPrice,
        long openPrice,
        long highPrice,
        long lowPrice,
        double changeRate,
        long volume,
        List<PriceLevel> askPrices,
        List<PriceLevel> bidPrices,
        Instant updatedAt
) {
    public static Quote of(String stockCode, long currentPrice, long openPrice, long highPrice, long lowPrice,
                           double changeRate, long volume,
                           List<PriceLevel> askPrices, List<PriceLevel> bidPrices, Instant updatedAt) {
        return new Quote(stockCode, currentPrice, openPrice, highPrice, lowPrice,
                changeRate, volume, askPrices, bidPrices, updatedAt);
    }
}
