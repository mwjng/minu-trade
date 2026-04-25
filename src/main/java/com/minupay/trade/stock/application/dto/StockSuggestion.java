package com.minupay.trade.stock.application.dto;

import com.minupay.trade.stock.infrastructure.search.StockSearchDocument;

public record StockSuggestion(
        String code,
        String name
) {
    public static StockSuggestion from(StockSearchDocument doc) {
        return new StockSuggestion(doc.getCode(), doc.getName());
    }
}
