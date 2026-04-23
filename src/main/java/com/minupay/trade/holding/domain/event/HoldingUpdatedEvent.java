package com.minupay.trade.holding.domain.event;

import com.minupay.trade.common.event.AbstractDomainEvent;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class HoldingUpdatedEvent extends AbstractDomainEvent {

    public static final String EVENT_TYPE = "HoldingUpdated";
    public static final String AGGREGATE_TYPE = "Holding";

    public enum Reason { BUY, SELL }

    private final Long userId;
    private final String stockCode;
    private final int quantity;
    private final BigDecimal avgPrice;
    private final Reason reason;

    private HoldingUpdatedEvent(String traceId, Long userId, String stockCode,
                                int quantity, BigDecimal avgPrice, Reason reason) {
        super(traceId);
        this.userId = userId;
        this.stockCode = stockCode;
        this.quantity = quantity;
        this.avgPrice = avgPrice;
        this.reason = reason;
    }

    public static HoldingUpdatedEvent of(Long userId, String stockCode, int quantity,
                                         BigDecimal avgPrice, Reason reason, String traceId) {
        return new HoldingUpdatedEvent(traceId, userId, stockCode, quantity, avgPrice, reason);
    }

    @Override public String getEventType()     { return EVENT_TYPE; }
    @Override public String getAggregateType() { return AGGREGATE_TYPE; }
    @Override public String getAggregateId()   { return userId + ":" + stockCode; }

    @Override
    public Object getPayload() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("userId", userId);
        p.put("stockCode", stockCode);
        p.put("quantity", quantity);
        p.put("avgPrice", avgPrice);
        p.put("reason", reason.name());
        return p;
    }
}
