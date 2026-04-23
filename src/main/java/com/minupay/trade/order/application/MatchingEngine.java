package com.minupay.trade.order.application;

import com.minupay.trade.order.application.dto.CancelOrderCommand;
import com.minupay.trade.order.application.dto.MatchCommand;
import com.minupay.trade.order.application.dto.OrderInfo;
import com.minupay.trade.order.domain.orderbook.OrderBook;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingEngine {

    private final MatchingProcessor processor;
    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();
    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();

    public CompletableFuture<Void> submit(MatchCommand cmd) {
        ExecutorService executor = executors.computeIfAbsent(cmd.stockCode(), this::newExecutor);
        OrderBook book = books.computeIfAbsent(cmd.stockCode(), OrderBook::new);
        return CompletableFuture.runAsync(() -> {
            try {
                processor.process(book, cmd);
            } catch (RuntimeException e) {
                log.error("matching failed orderId={} stock={}", cmd.orderId(), cmd.stockCode(), e);
                throw e;
            }
        }, executor);
    }

    public CompletableFuture<OrderInfo> cancel(CancelOrderCommand cmd) {
        ExecutorService executor = executors.computeIfAbsent(cmd.stockCode(), this::newExecutor);
        OrderBook book = books.computeIfAbsent(cmd.stockCode(), OrderBook::new);
        return CompletableFuture.supplyAsync(() -> processor.cancel(book, cmd), executor);
    }

    public OrderBook bookOf(String stockCode) {
        return books.computeIfAbsent(stockCode, OrderBook::new);
    }

    private ExecutorService newExecutor(String stockCode) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "matcher-" + stockCode);
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void shutdown() {
        executors.values().forEach(ExecutorService::shutdown);
        for (ExecutorService exec : executors.values()) {
            try {
                if (!exec.awaitTermination(5, TimeUnit.SECONDS)) exec.shutdownNow();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                exec.shutdownNow();
            }
        }
    }
}
