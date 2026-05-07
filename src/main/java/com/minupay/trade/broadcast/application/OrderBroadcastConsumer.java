package com.minupay.trade.broadcast.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.account.application.AccountLookup;
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
public class OrderBroadcastConsumer {

    public static final String CONSUMER_GROUP = "trade-broadcast-order";

    private final ObjectMapper objectMapper;
    private final StompFanoutPublisher fanoutPublisher;
    private final AccountLookup accountLookup;

    @KafkaListener(
            topics = {
                    KafkaConfig.TOPIC_ORDER_ACCEPTED,
                    KafkaConfig.TOPIC_ORDER_FILLED,
                    KafkaConfig.TOPIC_ORDER_CANCELLED,
                    KafkaConfig.TOPIC_ORDER_REJECTED
            },
            groupId = CONSUMER_GROUP,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            Long userId = resolveUserId(record);
            if (userId == null) {
                ack.acknowledge();
                return;
            }
            fanoutPublisher.publish(StompFanoutMessage.userMessage(
                    userId, StompDestinations.USER_QUEUE_ORDERS, record.value()));
            ack.acknowledge();
        } catch (Exception e) {
            log.warn("Order broadcast 처리 실패 topic={} key={}", record.topic(), record.key(), e);
            ack.acknowledge();
        }
    }

    private Long resolveUserId(ConsumerRecord<String, String> record) throws Exception {
        JsonNode envelope = objectMapper.readTree(record.value());
        JsonNode payload = envelope.path("payload");
        if (payload.has("userId") && !payload.path("userId").isNull()) {
            return payload.path("userId").asLong();
        }
        if (payload.has("accountId") && !payload.path("accountId").isNull()) {
            Long accountId = payload.path("accountId").asLong();
            return accountLookup.getUserId(accountId);
        }
        return null;
    }
}
