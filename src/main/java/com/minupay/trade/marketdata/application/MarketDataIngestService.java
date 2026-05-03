package com.minupay.trade.marketdata.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.event.EventEnvelope;
import com.minupay.trade.marketdata.application.dto.QuoteSnapshot;
import com.minupay.trade.marketdata.domain.Quote;
import com.minupay.trade.marketdata.event.QuoteUpdatedEvent;
import com.minupay.trade.marketdata.infrastructure.cache.QuoteCacheRepository;
import com.minupay.trade.marketdata.infrastructure.persistence.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataIngestService {

    private final QuoteRepository quoteRepository;
    private final QuoteCacheRepository quoteCacheRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void ingest(QuoteSnapshot snapshot, String traceId) {
        Quote quote = snapshot.toQuote();
        quoteRepository.save(quote);
        quoteCacheRepository.save(quote);
        publish(snapshot, traceId);
    }

    private void publish(QuoteSnapshot snapshot, String traceId) {
        QuoteUpdatedEvent event = QuoteUpdatedEvent.of(snapshot, traceId);
        try {
            String json = EventEnvelope.from(event).toJson(objectMapper);
            kafkaTemplate.send(KafkaConfig.TOPIC_QUOTE_UPDATED, snapshot.stockCode(), json);
        } catch (JsonProcessingException e) {
            log.error("quote.updated 직렬화 실패 stockCode={}", snapshot.stockCode(), e);
        }
    }
}
