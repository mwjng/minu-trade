package com.minupay.trade.marketdata.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.marketdata.domain.Quote;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class QuoteCacheRepository {

    private static final String KEY_PREFIX = "quote:";
    private static final Duration TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(Quote quote) {
        try {
            String json = objectMapper.writeValueAsString(quote);
            redisTemplate.opsForValue().set(keyOf(quote.stockCode()), json, TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Quote 직렬화 실패: " + quote.stockCode(), e);
        }
    }

    public Optional<Quote> findByStockCode(String stockCode) {
        String json = redisTemplate.opsForValue().get(keyOf(stockCode));
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, Quote.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Quote 역직렬화 실패: " + stockCode, e);
        }
    }

    private String keyOf(String stockCode) {
        return KEY_PREFIX + stockCode;
    }
}
