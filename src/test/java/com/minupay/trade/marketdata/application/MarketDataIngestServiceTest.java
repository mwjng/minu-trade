package com.minupay.trade.marketdata.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.marketdata.application.dto.QuoteSnapshot;
import com.minupay.trade.marketdata.domain.PriceLevel;
import com.minupay.trade.marketdata.domain.Quote;
import com.minupay.trade.marketdata.infrastructure.cache.QuoteCacheRepository;
import com.minupay.trade.marketdata.infrastructure.persistence.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MarketDataIngestServiceTest {

    @Mock QuoteRepository quoteRepository;
    @Mock QuoteCacheRepository quoteCacheRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    MarketDataIngestService service;

    @BeforeEach
    void setup() {
        service = new MarketDataIngestService(quoteRepository, quoteCacheRepository, kafkaTemplate, objectMapper);
    }

    private QuoteSnapshot snapshot(String code) {
        return new QuoteSnapshot(
                code, 75_000L, 74_000L, 75_500L, 73_800L, 1.35, 12_000_000L,
                List.of(PriceLevel.of(75_100L, 1_000L)),
                List.of(PriceLevel.of(74_900L, 800L)),
                Instant.parse("2026-04-25T09:00:00Z")
        );
    }

    @Test
    void 시세를_수신하면_MongoDB와_Redis와_Kafka에_각각_반영한다() {
        service.ingest(snapshot("005930"), "trace-1");

        ArgumentCaptor<Quote> mongoCaptor = ArgumentCaptor.forClass(Quote.class);
        verify(quoteRepository).save(mongoCaptor.capture());
        assertThat(mongoCaptor.getValue().stockCode()).isEqualTo("005930");
        assertThat(mongoCaptor.getValue().currentPrice()).isEqualTo(75_000L);

        ArgumentCaptor<Quote> redisCaptor = ArgumentCaptor.forClass(Quote.class);
        verify(quoteCacheRepository).save(redisCaptor.capture());
        assertThat(redisCaptor.getValue().stockCode()).isEqualTo("005930");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo(KafkaConfig.TOPIC_QUOTE_UPDATED);
        assertThat(keyCaptor.getValue()).isEqualTo("005930");
        assertThat(payloadCaptor.getValue()).contains("\"stockCode\":\"005930\"")
                .contains("\"currentPrice\":75000")
                .contains("\"eventType\":\"QuoteUpdated\"")
                .contains("\"traceId\":\"trace-1\"");
    }
}
