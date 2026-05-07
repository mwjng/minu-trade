package com.minupay.trade.broadcast.application;

public final class StompDestinations {

    public static final String QUOTE_TOPIC_PREFIX = "/topic/quotes/";

    public static final String USER_QUEUE_ORDERS = "/queue/orders";
    public static final String USER_QUEUE_TRADES = "/queue/trades";
    public static final String USER_QUEUE_HOLDINGS = "/queue/holdings";

    private StompDestinations() {
    }

    public static String quoteTopic(String stockCode) {
        return QUOTE_TOPIC_PREFIX + stockCode;
    }
}
