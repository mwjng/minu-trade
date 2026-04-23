package com.minupay.trade.order.domain.orderbook;

import com.minupay.trade.common.money.Money;
import com.minupay.trade.order.domain.OrderSide;
import lombok.Getter;

@Getter
public class BookEntry {

    private final long orderId;
    private final OrderSide side;
    private final Money price;
    private final long sequence;
    private int remainingQuantity;

    public BookEntry(long orderId, OrderSide side, Money price, int remainingQuantity, long sequence) {
        this.orderId = orderId;
        this.side = side;
        this.price = price;
        this.remainingQuantity = remainingQuantity;
        this.sequence = sequence;
    }

    void consume(int qty) {
        this.remainingQuantity -= qty;
    }

    public boolean isEmpty() {
        return remainingQuantity <= 0;
    }
}
