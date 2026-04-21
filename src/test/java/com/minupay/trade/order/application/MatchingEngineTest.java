package com.minupay.trade.order.application;

import com.minupay.trade.order.application.dto.MatchCommand;
import com.minupay.trade.order.domain.OrderSide;
import com.minupay.trade.order.domain.OrderType;
import com.minupay.trade.order.domain.orderbook.OrderBook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingEngineTest {

    private MatchingEngine engine;

    @AfterEach
    void tearDown() {
        if (engine != null) engine.shutdown();
    }

    @Test
    void 동일_종목은_하나의_스레드에서_순차처리() throws Exception {
        int n = 30;
        CountDownLatch started = new CountDownLatch(n);
        ConcurrentHashMap<Long, String> threadByOrder = new ConcurrentHashMap<>();

        MatchingProcessor processor = new RecordingProcessor((book, cmd) -> {
            threadByOrder.put(cmd.orderId(), Thread.currentThread().getName());
            started.countDown();
        });
        engine = new MatchingEngine(processor);

        CompletableFuture<?>[] futures = new CompletableFuture[n];
        for (int i = 0; i < n; i++) {
            futures[i] = engine.submit(cmd(i + 1L, "005930"));
        }
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

        Set<String> threads = Set.copyOf(threadByOrder.values());
        assertThat(threads).hasSize(1);
        assertThat(threads.iterator().next()).isEqualTo("matcher-005930");
    }

    @Test
    void 다른_종목은_서로_다른_스레드() throws Exception {
        MatchingProcessor processor = new RecordingProcessor((book, cmd) -> { });
        engine = new MatchingEngine(processor);

        engine.submit(cmd(1L, "005930")).get(2, TimeUnit.SECONDS);
        engine.submit(cmd(2L, "000660")).get(2, TimeUnit.SECONDS);

        assertThat(engine.bookOf("005930")).isNotSameAs(engine.bookOf("000660"));
    }

    @Test
    void 동시에_여러_종목_요청도_모두_완료() throws Exception {
        List<String> codes = List.of("005930", "000660", "035720", "051910");
        ConcurrentHashMap<String, Integer> countByStock = new ConcurrentHashMap<>();

        MatchingProcessor processor = new RecordingProcessor((book, cmd) ->
                countByStock.merge(cmd.stockCode(), 1, Integer::sum));
        engine = new MatchingEngine(processor);

        CompletableFuture<?>[] futures = new CompletableFuture[codes.size() * 10];
        int idx = 0;
        for (int i = 0; i < 10; i++) {
            for (String code : codes) {
                futures[idx++] = engine.submit(cmd((long) idx, code));
            }
        }
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

        for (String code : codes) {
            assertThat(countByStock.get(code)).isEqualTo(10);
        }
    }

    private MatchCommand cmd(long orderId, String stockCode) {
        return new MatchCommand(orderId, stockCode, OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("70000"), 1);
    }

    @FunctionalInterface
    private interface ProcessFn {
        void apply(OrderBook book, MatchCommand cmd);
    }

    private static class RecordingProcessor extends MatchingProcessor {
        private final ProcessFn fn;
        RecordingProcessor(ProcessFn fn) {
            super(null, null, null, null);
            this.fn = fn;
        }
        @Override
        public com.minupay.trade.order.domain.orderbook.MatchResult process(OrderBook book, MatchCommand cmd) {
            fn.apply(book, cmd);
            return new com.minupay.trade.order.domain.orderbook.MatchResult(List.of(), cmd.quantity(), false);
        }
    }
}
