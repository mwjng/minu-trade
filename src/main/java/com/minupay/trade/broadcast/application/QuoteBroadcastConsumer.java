package com.minupay.trade.broadcast.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.common.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteBroadcastConsumer {

    public static final String CONSUMER_GROUP = "trade-broadcast-quote";

    private final ObjectMapper objectMapper;
    private final StompFanoutPublisher fanoutPublisher;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_QUOTE_UPDATED,
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            String stockCode = stockCodeFrom(record);
            if (stockCode == null) {
                ack.acknowledge();
                return;
            }
            fanoutPublisher.publish(StompFanoutMessage.publicMessage(
                    StompDestinations.quoteTopic(stockCode), record.value()));
            ack.acknowledge();
        } catch (Exception e) {
            log.warn("Quote broadcast 처리 실패 key={}", record.key(), e);
            ack.acknowledge();
        }
    }

    private String stockCodeFrom(ConsumerRecord<String, String> record) throws Exception {
        if (record.key() != null && !record.key().isBlank()) {
            return record.key();
        }
        JsonNode envelope = objectMapper.readTree(record.value());
        JsonNode payload = envelope.path("payload");
        String stockCode = payload.path("stockCode").asText(null);
        return (stockCode == null || stockCode.isBlank()) ? null : stockCode;
    }
}
