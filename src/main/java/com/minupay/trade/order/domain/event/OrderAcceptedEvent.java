package com.minupay.trade.order.domain.event;

import com.minupay.trade.common.event.AbstractDomainEvent;
import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderSide;
import com.minupay.trade.order.domain.OrderType;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrderAcceptedEvent extends AbstractDomainEvent {

    public static final String EVENT_TYPE = "OrderAccepted";
    public static final String AGGREGATE_TYPE = "Order";

    private final Long orderId;
    private final Long accountId;
    private final String stockCode;
    private final OrderSide side;
    private final OrderType type;
    private final BigDecimal price;
    private final int quantity;

    private OrderAcceptedEvent(String traceId, Order order) {
        super(traceId);
        this.orderId = order.getId();
        this.accountId = order.getAccountId();
        this.stockCode = order.getStockCode();
        this.side = order.getSide();
        this.type = order.getType();
        this.price = order.getPrice();
        this.quantity = order.getQuantity();
    }

    public static OrderAcceptedEvent of(Order order, String traceId) {
        return new OrderAcceptedEvent(traceId, order);
    }

    @Override public String getEventType()     { return EVENT_TYPE; }
    @Override public String getAggregateType() { return AGGREGATE_TYPE; }
    @Override public String getAggregateId()   { return String.valueOf(orderId); }

    @Override
    public Object getPayload() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("orderId", orderId);
        p.put("accountId", accountId);
        p.put("stockCode", stockCode);
        p.put("side", side.name());
        p.put("type", type.name());
        p.put("price", price);
        p.put("quantity", quantity);
        return p;
    }
}
