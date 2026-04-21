package com.minupay.trade.order.domain.event;

import com.minupay.trade.common.event.AbstractDomainEvent;
import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderSide;
import com.minupay.trade.order.domain.OrderStatus;

import java.util.LinkedHashMap;
import java.util.Map;

public class OrderFilledEvent extends AbstractDomainEvent {

    public static final String EVENT_TYPE = "OrderFilled";
    public static final String AGGREGATE_TYPE = "Order";

    private final Long orderId;
    private final Long accountId;
    private final String stockCode;
    private final OrderSide side;
    private final int quantity;
    private final int filledQuantity;
    private final OrderStatus status;

    private OrderFilledEvent(String traceId, Order order) {
        super(traceId);
        this.orderId = order.getId();
        this.accountId = order.getAccountId();
        this.stockCode = order.getStockCode();
        this.side = order.getSide();
        this.quantity = order.getQuantity();
        this.filledQuantity = order.getFilledQuantity();
        this.status = order.getStatus();
    }

    public static OrderFilledEvent of(Order order, String traceId) {
        return new OrderFilledEvent(traceId, order);
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
        p.put("quantity", quantity);
        p.put("filledQuantity", filledQuantity);
        p.put("status", status.name());
        return p;
    }
}
