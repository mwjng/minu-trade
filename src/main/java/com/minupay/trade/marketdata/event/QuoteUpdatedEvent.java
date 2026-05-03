package com.minupay.trade.marketdata.event;

import com.minupay.trade.common.event.AbstractDomainEvent;
import com.minupay.trade.marketdata.application.dto.QuoteSnapshot;
import com.minupay.trade.marketdata.domain.PriceLevel;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuoteUpdatedEvent extends AbstractDomainEvent {

    public static final String EVENT_TYPE = "QuoteUpdated";
    public static final String AGGREGATE_TYPE = "Quote";

    private final String stockCode;
    private final long currentPrice;
    private final long openPrice;
    private final long highPrice;
    private final long lowPrice;
    private final double changeRate;
    private final long volume;
    private final List<PriceLevel> askPrices;
    private final List<PriceLevel> bidPrices;
    private final Instant occurredAt;

    private QuoteUpdatedEvent(String traceId, QuoteSnapshot snapshot) {
        super(traceId);
        this.stockCode = snapshot.stockCode();
        this.currentPrice = snapshot.currentPrice();
        this.openPrice = snapshot.openPrice();
        this.highPrice = snapshot.highPrice();
        this.lowPrice = snapshot.lowPrice();
        this.changeRate = snapshot.changeRate();
        this.volume = snapshot.volume();
        this.askPrices = snapshot.askPrices();
        this.bidPrices = snapshot.bidPrices();
        this.occurredAt = snapshot.occurredAt();
    }

    public static QuoteUpdatedEvent of(QuoteSnapshot snapshot, String traceId) {
        return new QuoteUpdatedEvent(traceId, snapshot);
    }

    @Override public String getEventType()     { return EVENT_TYPE; }
    @Override public String getAggregateType() { return AGGREGATE_TYPE; }
    @Override public String getAggregateId()   { return stockCode; }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stockCode", stockCode);
        payload.put("currentPrice", currentPrice);
        payload.put("openPrice", openPrice);
        payload.put("highPrice", highPrice);
        payload.put("lowPrice", lowPrice);
        payload.put("changeRate", changeRate);
        payload.put("volume", volume);
        payload.put("askPrices", askPrices);
        payload.put("bidPrices", bidPrices);
        payload.put("occurredAt", occurredAt.toString());
        return payload;
    }
}
