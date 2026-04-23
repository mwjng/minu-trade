package com.minupay.trade.order.domain.event;

import com.minupay.trade.common.event.AbstractDomainEvent;
import com.minupay.trade.order.domain.Execution;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class TradeExecutedEvent extends AbstractDomainEvent {

    public static final String EVENT_TYPE = "TradeExecuted";
    public static final String AGGREGATE_TYPE = "Execution";

    private final Long executionId;
    private final Long buyOrderId;
    private final Long sellOrderId;
    private final Long buyerUserId;
    private final Long sellerUserId;
    private final String stockCode;
    private final BigDecimal price;
    private final int quantity;

    private TradeExecutedEvent(String traceId, Execution execution, Long buyerUserId, Long sellerUserId) {
        super(traceId);
        this.executionId = execution.getId();
        this.buyOrderId = execution.getBuyOrderId();
        this.sellOrderId = execution.getSellOrderId();
        this.buyerUserId = buyerUserId;
        this.sellerUserId = sellerUserId;
        this.stockCode = execution.getStockCode();
        this.price = execution.getPrice();
        this.quantity = execution.getQuantity();
    }

    public static TradeExecutedEvent of(Execution execution, Long buyerUserId, Long sellerUserId, String traceId) {
        return new TradeExecutedEvent(traceId, execution, buyerUserId, sellerUserId);
    }

    @Override public String getEventType()     { return EVENT_TYPE; }
    @Override public String getAggregateType() { return AGGREGATE_TYPE; }
    @Override public String getAggregateId()   { return String.valueOf(executionId); }

    @Override
    public Object getPayload() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("executionId", executionId);
        p.put("buyOrderId", buyOrderId);
        p.put("sellOrderId", sellOrderId);
        p.put("buyerUserId", buyerUserId);
        p.put("sellerUserId", sellerUserId);
        p.put("stockCode", stockCode);
        p.put("price", price);
        p.put("quantity", quantity);
        return p;
    }
}
