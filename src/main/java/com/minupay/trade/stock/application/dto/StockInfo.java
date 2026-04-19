package com.minupay.trade.stock.application.dto;

import com.minupay.trade.stock.domain.Market;
import com.minupay.trade.stock.domain.Stock;
import com.minupay.trade.stock.domain.StockStatus;

import java.time.LocalDate;

public record StockInfo(
        String code,
        String name,
        Market market,
        String sector,
        int tickSize,
        Long marketCap,
        StockStatus status,
        LocalDate listedAt
) {
    public static StockInfo from(Stock stock) {
        return new StockInfo(
                stock.getCode(),
                stock.getName(),
                stock.getMarket(),
                stock.getSector(),
                stock.getTickSize(),
                stock.getMarketCap(),
                stock.getStatus(),
                stock.getListedAt()
        );
    }
}
