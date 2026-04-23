package com.minupay.trade.order.domain.event;

import com.minupay.trade.common.event.AbstractDomainEvent;
import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderSide;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrderCancelledEvent extends AbstractDomainEvent {

    public static final String EVENT_TYPE = "OrderCancelled";
    public static final String AGGREGATE_TYPE = "Order";

    private final Long orderId;
    private final Long accountId;
    private final String stockCode;
    private final OrderSide side;
    private final BigDecimal price;
    private final int quantity;
    private final int filledQuantity;
    private final int cancelledQuantity;

    private OrderCancelledEvent(String traceId, Order order, int cancelledQuantity) {
        super(traceId);
        this.orderId = order.getId();
        this.accountId = order.getAccountId();
        this.stockCode = order.getStockCode();
        this.side = order.getSide();
        this.price = order.getPrice();
        this.quantity = order.getQuantity();
        this.filledQuantity = order.getFilledQuantity();
        this.cancelledQuantity = cancelledQuantity;
    }

    public static OrderCancelledEvent of(Order order, int cancelledQuantity, String traceId) {
        return new OrderCancelledEvent(traceId, order, cancelledQuantity);
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_TYPE;
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(orderId);
    }

    @Override
    public Object getPayload() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("orderId", orderId);
        p.put("accountId", accountId);
        p.put("stockCode", stockCode);
        p.put("side", side.name());
        p.put("price", price);
        p.put("quantity", quantity);
        p.put("filledQuantity", filledQuantity);
        p.put("cancelledQuantity", cancelledQuantity);
        return p;
    }
}
