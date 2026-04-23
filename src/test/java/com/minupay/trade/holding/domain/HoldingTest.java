package com.minupay.trade.holding.domain;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HoldingTest {

    @Test
    void openForBuy_초기수량_평단_세팅() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));

        assertThat(h.getQuantity()).isEqualTo(10);
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
        // (3*10000 + 4*12345) / 7 = 79380/7 = 11340.0000
        assertThat(h.getAvgPrice()).isEqualByComparingTo("11340.0000");
    }

    @Test
    void sell_수량_차감() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));

        h.sell(3);

        assertThat(h.getQuantity()).isEqualTo(7);
        assertThat(h.getAvgPrice()).isEqualByComparingTo("70000");
        assertThat(h.isEmpty()).isFalse();
    }

    @Test
    void sell_전량_후_isEmpty() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));

        h.sell(10);

        assertThat(h.isEmpty()).isTrue();
    }

    @Test
    void sell_보유보다_많이_요청하면_예외() {
        Holding h = Holding.openForBuy(1L, "005930", 10, new BigDecimal("70000"));

        assertThatThrownBy(() -> h.sell(11))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.HOLDING_INSUFFICIENT);
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
