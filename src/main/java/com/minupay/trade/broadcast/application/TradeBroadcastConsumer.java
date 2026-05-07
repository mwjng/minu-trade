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
public class TradeBroadcastConsumer {

    public static final String CONSUMER_GROUP = "trade-broadcast-trade";

    private final ObjectMapper objectMapper;
    private final StompFanoutPublisher fanoutPublisher;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_TRADE_EXECUTED,
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode payload = objectMapper.readTree(record.value()).path("payload");
            sendToUser(payload, "buyerUserId", record.value());
            sendToUser(payload, "sellerUserId", record.value());
            ack.acknowledge();
        } catch (Exception e) {
            log.warn("Trade broadcast 처리 실패 key={}", record.key(), e);
            ack.acknowledge();
        }
    }

    private void sendToUser(JsonNode payload, String userIdField, String envelopeJson) {
        if (!payload.has(userIdField) || payload.path(userIdField).isNull()) {
            return;
        }
        Long userId = payload.path(userIdField).asLong();
        fanoutPublisher.publish(StompFanoutMessage.userMessage(
                userId, StompDestinations.USER_QUEUE_TRADES, envelopeJson));
    }
}
