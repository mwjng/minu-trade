package com.minupay.trade.holding.domain;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HoldingTest {

    @Test
    void openForBuy_초기수량_평단_세팅_예약수량_0() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));

        assertThat(h.getQuantity()).isEqualTo(10);
        assertThat(h.getReservedQuantity()).isZero();
        assertThat(h.availableQuantity()).isEqualTo(10);
        assertThat(h.getAvgPrice()).isEqualByComparingTo("70000");
        assertThat(h.getUserId()).isEqualTo(1L);
        assertThat(h.getStockCode()).isEqualTo("005930");
    }

    @Test
    void buy_가중평균_갱신() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));

        h.buy(10, new BigDecimal("80000"));

        assertThat(h.getQuantity()).isEqualTo(20);
        assertThat(h.getAvgPrice()).isEqualByComparingTo("75000");
    }

    @Test
    void buy_소수점_평단_반올림() {
        Holding h = Holding.openForBuy(1L, "005930", 3, new BigDecimal("10000"));

        h.buy(4, new BigDecimal("12345"));

        assertThat(h.getQuantity()).isEqualTo(7);
        assertThat(h.getAvgPrice()).isEqualByComparingTo("11340.0000");
    }

    @Test
    void reserve_가용수량에서_예약() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));

        h.reserve(3);

        assertThat(h.getQuantity()).isEqualTo(10);
        assertThat(h.getReservedQuantity()).isEqualTo(3);
        assertThat(h.availableQuantity()).isEqualTo(7);
    }

    @Test
    void reserve_가용수량_부족시_예외() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));
        h.reserve(7);

        assertThatThrownBy(() -> h.reserve(4))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HOLDING_INSUFFICIENT);
    }

    @Test
    void releaseReserve_예약_해제() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));
        h.reserve(5);

        h.releaseReserve(2);

        assertThat(h.getReservedQuantity()).isEqualTo(3);
        assertThat(h.availableQuantity()).isEqualTo(7);
    }

    @Test
    void releaseReserve_예약보다_크면_예외() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));
        h.reserve(3);

        assertThatThrownBy(() -> h.releaseReserve(4))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HOLDING_INSUFFICIENT_RESERVED);
    }

    @Test
    void settleSell_예약과_보유수량_동시_차감() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));
        h.reserve(5);

        h.settleSell(3);

        assertThat(h.getQuantity()).isEqualTo(7);
        assertThat(h.getReservedQuantity()).isEqualTo(2);
        assertThat(h.availableQuantity()).isEqualTo(5);
        assertThat(h.isEmpty()).isFalse();
    }

    @Test
    void settleSell_예약된_전량_소진_후_isEmpty() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));
        h.reserve(10);

        h.settleSell(10);

        assertThat(h.getQuantity()).isZero();
        assertThat(h.getReservedQuantity()).isZero();
        assertThat(h.isEmpty()).isTrue();
    }

    @Test
    void settleSell_예약보다_크면_예외() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));
        h.reserve(3);

        assertThatThrownBy(() -> h.settleSell(4))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HOLDING_INSUFFICIENT_RESERVED);
    }

    @Test
    void buy_0_이하_수량이면_예외() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));

        assertThatThrownBy(() -> h.buy(0, new BigDecimal("70000")))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HOLDING_INVALID_QUANTITY);
    }

    @Test
    void buy_0_이하_가격이면_예외() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));

        assertThatThrownBy(() -> h.buy(1, BigDecimal.ZERO))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HOLDING_INVALID_PRICE);
    }
}
