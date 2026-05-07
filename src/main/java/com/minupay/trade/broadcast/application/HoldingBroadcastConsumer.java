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
public class HoldingBroadcastConsumer {

    public static final String CONSUMER_GROUP = "trade-broadcast-holding";

    private final ObjectMapper objectMapper;
    private final StompFanoutPublisher fanoutPublisher;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_HOLDING_UPDATED,
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode payload = objectMapper.readTree(record.value()).path("payload");
            if (!payload.has("userId") || payload.path("userId").isNull()) {
                ack.acknowledge();
                return;
            }
            Long userId = payload.path("userId").asLong();
            fanoutPublisher.publish(StompFanoutMessage.userMessage(
                    userId, StompDestinations.USER_QUEUE_HOLDINGS, record.value()));
            ack.acknowledge();
        } catch (Exception e) {
            log.warn("Holding broadcast 처리 실패 key={}", record.key(), e);
            ack.acknowledge();
        }
    }
}
