package com.minupay.trade.order.domain;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private Order sampleLimitBuy() {
        return Order.place(1L, "005930", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("70000"), 10, "idem-1");
    }

    @Test
    void 생성_시_PENDING_상태() {
        Order order = sampleLimitBuy();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getFilledQuantity()).isZero();
    }

    @Test
    void accept_후_ACCEPTED_paymentId_저장() {
        Order order = sampleLimitBuy();
        order.accept(42L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(order.getPaymentId()).isEqualTo(42L);
    }

    @Test
    void 수량_0이하면_예외() {
        assertThatThrownBy(() -> Order.place(1L, "005930", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("70000"), 0, "idem"))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_INVALID_QUANTITY);
    }

    @Test
    void LIMIT_주문에_가격이_없거나_음수면_예외() {
        assertThatThrownBy(() -> Order.place(1L, "005930", OrderSide.BUY, OrderType.LIMIT,
                null, 10, "idem"))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_INVALID_PRICE);

        assertThatThrownBy(() -> Order.place(1L, "005930", OrderSide.BUY, OrderType.LIMIT,
                BigDecimal.ZERO, 10, "idem"))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_INVALID_PRICE);
    }

    @Test
    void MARKET_주문에_가격이_있으면_예외() {
        assertThatThrownBy(() -> Order.place(1L, "005930", OrderSide.BUY, OrderType.MARKET,
                new BigDecimal("70000"), 10, "idem"))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_INVALID_PRICE);
    }

    @Test
    void 부분체결_후_PARTIALLY_FILLED() {
        Order order = sampleLimitBuy();
        order.accept(1L);
        order.addFill(4);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(order.getFilledQuantity()).isEqualTo(4);
        assertThat(order.remainingQuantity()).isEqualTo(6);
    }

    @Test
    void 전량체결_후_FILLED() {
        Order order = sampleLimitBuy();
        order.accept(1L);
        order.addFill(10);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.remainingQuantity()).isZero();
    }

    @Test
    void 초과체결_시도_시_예외() {
        Order order = sampleLimitBuy();
        order.accept(1L);
        order.addFill(8);
        assertThatThrownBy(() -> order.addFill(3))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_OVERFILL);
    }

    @Test
    void 체결된_주문은_취소_불가() {
        Order order = sampleLimitBuy();
        order.accept(1L);
        order.addFill(10);
        assertThatThrownBy(order::cancel)
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_INVALID_STATE);
    }

    @Test
    void totalAmount_는_가격x수량() {
        Order order = sampleLimitBuy();
        assertThat(order.totalAmount()).isEqualByComparingTo(new BigDecimal("700000"));
    }
}
