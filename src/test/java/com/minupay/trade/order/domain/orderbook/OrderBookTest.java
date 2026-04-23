package com.minupay.trade.order.domain.orderbook;

import com.minupay.trade.common.money.Money;
import com.minupay.trade.order.domain.OrderSide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderBookTest {

    private static Money p(String s) {
        return Money.of(new BigDecimal(s));
    }

    @Test
    void 가격_안맞으면_호가창에_등록만_되고_체결없음() {
        OrderBook book = new OrderBook("005930");

        MatchResult r1 = book.match(1L, OrderSide.SELL, p("70100"), 10);
        MatchResult r2 = book.match(2L, OrderSide.BUY, p("70000"), 5);

        assertThat(r1.trades()).isEmpty();
        assertThat(r1.restingEntryAdded()).isTrue();
        assertThat(r2.trades()).isEmpty();
        assertThat(r2.restingEntryAdded()).isTrue();
        assertThat(book.bestAsk()).map(Money::getAmount).get().isEqualTo(new BigDecimal("70100"));
        assertThat(book.bestBid()).map(Money::getAmount).get().isEqualTo(new BigDecimal("70000"));
    }

    @Test
    void 가격교차시_즉시_전량체결() {
        OrderBook book = new OrderBook("005930");
        book.match(1L, OrderSide.SELL, p("70000"), 10);

        MatchResult taker = book.match(2L, OrderSide.BUY, p("70000"), 10);

        assertThat(taker.trades()).hasSize(1);
        Trade t = taker.trades().get(0);
        assertThat(t.buyOrderId()).isEqualTo(2L);
        assertThat(t.sellOrderId()).isEqualTo(1L);
        assertThat(t.price().getAmount()).isEqualByComparingTo("70000");
        assertThat(t.quantity()).isEqualTo(10);
        assertThat(taker.restingEntryAdded()).isFalse();
        assertThat(book.bestAsk()).isEmpty();
    }

    @Test
    void 부분체결_후_잔량_호가창_등록() {
        OrderBook book = new OrderBook("005930");
        book.match(1L, OrderSide.SELL, p("70000"), 4);

        MatchResult taker = book.match(2L, OrderSide.BUY, p("70000"), 10);

        assertThat(taker.trades()).hasSize(1);
        assertThat(taker.trades().get(0).quantity()).isEqualTo(4);
        assertThat(taker.remainingQuantity()).isEqualTo(6);
        assertThat(taker.restingEntryAdded()).isTrue();
        assertThat(book.bestBid()).map(Money::getAmount).get().isEqualTo(new BigDecimal("70000"));
        assertThat(book.bestAsk()).isEmpty();
    }

    @Test
    void 여러_메이커에_걸쳐_다중체결() {
        OrderBook book = new OrderBook("005930");
        book.match(1L, OrderSide.SELL, p("70000"), 3);
        book.match(2L, OrderSide.SELL, p("70000"), 4);
        book.match(3L, OrderSide.SELL, p("70100"), 5);

        MatchResult taker = book.match(10L, OrderSide.BUY, p("70100"), 10);

        assertThat(taker.trades()).hasSize(3);
        assertThat(taker.trades().get(0).sellOrderId()).isEqualTo(1L);
        assertThat(taker.trades().get(0).price().getAmount()).isEqualByComparingTo("70000");
        assertThat(taker.trades().get(1).sellOrderId()).isEqualTo(2L);
        assertThat(taker.trades().get(2).sellOrderId()).isEqualTo(3L);
        assertThat(taker.trades().get(2).quantity()).isEqualTo(3);
        assertThat(taker.remainingQuantity()).isZero();
        assertThat(book.bestAsk()).map(Money::getAmount).get().isEqualTo(new BigDecimal("70100"));
    }

    @Test
    void 같은_가격은_FIFO_순으로_체결() {
        OrderBook book = new OrderBook("005930");
        book.match(1L, OrderSide.SELL, p("70000"), 5);
        book.match(2L, OrderSide.SELL, p("70000"), 5);

        MatchResult taker = book.match(3L, OrderSide.BUY, p("70000"), 6);

        assertThat(taker.trades()).hasSize(2);
        assertThat(taker.trades().get(0).sellOrderId()).isEqualTo(1L);
        assertThat(taker.trades().get(0).quantity()).isEqualTo(5);
        assertThat(taker.trades().get(1).sellOrderId()).isEqualTo(2L);
        assertThat(taker.trades().get(1).quantity()).isEqualTo(1);
    }

    @Test
    void 매수는_낮은_호가부터_소진() {
        OrderBook book = new OrderBook("005930");
        book.match(1L, OrderSide.SELL, p("70200"), 5);
        book.match(2L, OrderSide.SELL, p("70000"), 5);

        MatchResult taker = book.match(3L, OrderSide.BUY, p("70200"), 10);

        assertThat(taker.trades().get(0).sellOrderId()).isEqualTo(2L);
        assertThat(taker.trades().get(0).price().getAmount()).isEqualByComparingTo("70000");
        assertThat(taker.trades().get(1).sellOrderId()).isEqualTo(1L);
        assertThat(taker.trades().get(1).price().getAmount()).isEqualByComparingTo("70200");
    }

    @Test
    void 매도는_높은_호가부터_소진() {
        OrderBook book = new OrderBook("005930");
        book.match(1L, OrderSide.BUY, p("69800"), 5);
        book.match(2L, OrderSide.BUY, p("70000"), 5);

        MatchResult taker = book.match(3L, OrderSide.SELL, p("69800"), 10);

        assertThat(taker.trades().get(0).buyOrderId()).isEqualTo(2L);
        assertThat(taker.trades().get(0).price().getAmount()).isEqualByComparingTo("70000");
        assertThat(taker.trades().get(1).buyOrderId()).isEqualTo(1L);
    }

    @Test
    void 취소시_호가창에서_제거() {
        OrderBook book = new OrderBook("005930");
        book.match(1L, OrderSide.SELL, p("70000"), 10);

        boolean removed = book.cancel(1L);

        assertThat(removed).isTrue();
        assertThat(book.bestAsk()).isEmpty();
        assertThat(book.depth(OrderSide.SELL)).isZero();
    }

    @Test
    void 없는_주문_취소는_false() {
        OrderBook book = new OrderBook("005930");
        assertThat(book.cancel(999L)).isFalse();
    }
}
