package com.minupay.trade.stock.domain;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    private Stock sample() {
        return Stock.register("005930", "삼성전자", Market.KOSPI, "전기전자", 100, 400_000_000_000_000L, LocalDate.of(1975, 6, 11));
    }

    @Test
    void 등록_시_TRADING_상태() {
        Stock stock = sample();
        assertThat(stock.getStatus()).isEqualTo(StockStatus.TRADING);
        assertThat(stock.isTradable()).isTrue();
    }

    @Test
    void 틱사이즈는_양수여야_함() {
        assertThatThrownBy(() ->
                Stock.register("000000", "X", Market.KOSPI, "Y", 0, 0L, LocalDate.now())
        ).isInstanceOf(MinuTradeException.class)
         .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void 거래정지_후_재개() {
        Stock stock = sample();
        stock.halt();
        assertThat(stock.getStatus()).isEqualTo(StockStatus.HALTED);
        assertThat(stock.isTradable()).isFalse();

        stock.resume();
        assertThat(stock.getStatus()).isEqualTo(StockStatus.TRADING);
    }

    @Test
    void 상장폐지된_종목은_정지_불가() {
        Stock stock = sample();
        stock.delist();

        assertThatThrownBy(stock::halt)
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STOCK_DELISTED);
    }

    @Test
    void 상장폐지된_종목은_재개_불가() {
        Stock stock = sample();
        stock.delist();

        assertThatThrownBy(stock::resume)
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STOCK_DELISTED);
    }

    @Test
    void 프로필_업데이트() {
        Stock stock = sample();
        stock.updateProfile("삼성전자우", "전기전자", 50, 500_000_000_000_000L);
        assertThat(stock.getName()).isEqualTo("삼성전자우");
        assertThat(stock.getTickSize()).isEqualTo(50);
        assertThat(stock.getMarketCap()).isEqualTo(500_000_000_000_000L);
    }
}
