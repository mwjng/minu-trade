package com.minupay.trade.marketdata.infrastructure.kis;

import com.minupay.trade.marketdata.application.MarketDataIngestService;
import com.minupay.trade.marketdata.application.dto.QuoteSnapshot;
import com.minupay.trade.marketdata.domain.PriceLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kis.mock", name = "enabled", havingValue = "true")
public class MockKisQuoteSource {

    private static final int ORDER_BOOK_DEPTH = 10;

    private final MarketDataIngestService marketDataIngestService;
    private final Map<String, Long> nextEmitAt = new HashMap<>();

    @Value("${kis.mock.stock-codes:005930,000660,035720,005380,051910}")
    private List<String> stockCodes;

    @Value("${kis.mock.interval-ms:500}")
    private long baseIntervalMs;

    @Value("${kis.mock.jitter-ratio:0.5}")
    private double jitterRatio;

    @Scheduled(fixedDelay = 100)
    public void emit() {
        long now = System.currentTimeMillis();
        for (String stockCode : stockCodes) {
            if (!isDue(stockCode, now)) continue;
            try {
                marketDataIngestService.ingest(generate(stockCode), UUID.randomUUID().toString());
            } catch (Exception e) {
                log.warn("Mock 시세 발행 실패 stockCode={}", stockCode, e);
            }
            nextEmitAt.put(stockCode, now + nextDelayMs());
        }
    }

    private boolean isDue(String stockCode, long now) {
        Long scheduledAt = nextEmitAt.get(stockCode);
        return scheduledAt == null || now >= scheduledAt;
    }

    private long nextDelayMs() {
        double low = baseIntervalMs * (1.0 - jitterRatio);
        double high = baseIntervalMs * (1.0 + jitterRatio);
        return (long) ThreadLocalRandom.current().nextDouble(low, high);
    }

    private QuoteSnapshot generate(String stockCode) {
        long basePrice = 50_000 + ThreadLocalRandom.current().nextLong(50_000);
        long current = basePrice + ThreadLocalRandom.current().nextLong(-500, 500);
        long open = basePrice;
        long high = Math.max(current, basePrice + 300);
        long low = Math.min(current, basePrice - 300);
        double changeRate = (current - open) * 100.0 / open;
        long volume = ThreadLocalRandom.current().nextLong(1_000, 100_000);
        return new QuoteSnapshot(
                stockCode, current, open, high, low, changeRate, volume,
                priceLevels(current, +1), priceLevels(current, -1),
                Instant.now()
        );
    }

    private List<PriceLevel> priceLevels(long center, int direction) {
        List<PriceLevel> levels = new ArrayList<>(ORDER_BOOK_DEPTH);
        for (int i = 1; i <= ORDER_BOOK_DEPTH; i++) {
            long price = center + (direction * 100L * i);
            long quantity = ThreadLocalRandom.current().nextLong(100, 5_000);
            levels.add(PriceLevel.of(price, quantity));
        }
        return levels;
    }
}
