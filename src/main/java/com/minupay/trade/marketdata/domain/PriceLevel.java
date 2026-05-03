package com.minupay.trade.marketdata.domain;

public record PriceLevel(long price, long quantity) {
    public static PriceLevel of(long price, long quantity) {
        return new PriceLevel(price, quantity);
    }
}
