package com.minupay.trade.stock.infrastructure.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.consumer.ConsumedEventRecorder;
import com.minupay.trade.stock.domain.Market;
import com.minupay.trade.stock.domain.StockStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSearchIndexer {

    public static final String CONSUMER_GROUP = "trade-stock-search-indexer";

    private final ObjectMapper objectMapper;
    private final ConsumedEventRecorder consumedEventRecorder;
    private final StockSearchRepository stockSearchRepository;

    @Transactional
    @KafkaListener(
            topics = KafkaConfig.TOPIC_STOCK_UPDATED,
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode envelope = objectMapper.readTree(record.value());
            String eventId = envelope.path("eventId").asText();

            if (!consumedEventRecorder.markIfAbsent(eventId, CONSUMER_GROUP, record.topic())) {
                ack.acknowledge();
                return;
            }

            JsonNode payload = envelope.path("payload");
            StockSearchDocument doc = StockSearchDocument.of(
                    payload.path("code").asText(),
                    payload.path("name").asText(),
                    Market.valueOf(payload.path("market").asText()),
                    payload.path("sector").isNull() ? null : payload.path("sector").asText(null),
                    payload.path("tickSize").asInt(),
                    payload.path("marketCap").isNull() ? null : payload.path("marketCap").asLong(),
                    StockStatus.valueOf(payload.path("status").asText()),
                    LocalDate.parse(payload.path("listedAt").asText())
            );
            stockSearchRepository.save(doc);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Stock search indexing failed key={} offset={}", record.key(), record.offset(), e);
            throw new IllegalStateException("Stock search indexing failed", e);
        }
    }
}
