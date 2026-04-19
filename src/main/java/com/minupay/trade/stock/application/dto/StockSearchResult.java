package com.minupay.trade.stock.application.dto;

import com.minupay.trade.stock.domain.Market;
import com.minupay.trade.stock.domain.StockStatus;
import com.minupay.trade.stock.infrastructure.search.StockSearchDocument;

public record StockSearchResult(
        String code,
        String name,
        Market market,
        String sector,
        StockStatus status
) {
    public static StockSearchResult from(StockSearchDocument doc) {
        return new StockSearchResult(doc.getCode(), doc.getName(), doc.getMarket(), doc.getSector(), doc.getStatus());
    }
}
