package com.minupay.trade.audit.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.audit.domain.AuditLog;
import com.minupay.trade.audit.infrastructure.AuditLogRepository;
import com.minupay.trade.common.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final AuditLogRepository auditLogRepository;

    @KafkaListener(
            topics = {
                    KafkaConfig.TOPIC_ORDER_ACCEPTED,
                    KafkaConfig.TOPIC_ORDER_REJECTED,
                    KafkaConfig.TOPIC_ORDER_FILLED,
                    KafkaConfig.TOPIC_ORDER_CANCELLED,
                    KafkaConfig.TOPIC_TRADE_EXECUTED,
                    KafkaConfig.TOPIC_HOLDING_UPDATED,
                    KafkaConfig.TOPIC_STOCK_UPDATED
            },
            groupId = "${audit.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            persist(record);
        } catch (DuplicateKeyException e) {
            log.debug("audit 중복 이벤트 skip topic={} key={}", record.topic(), record.key());
        } catch (Exception e) {
            log.warn("audit 적재 실패 topic={} key={}", record.topic(), record.key(), e);
        } finally {
            ack.acknowledge();
        }
    }

    private void persist(ConsumerRecord<String, String> record) throws Exception {
        JsonNode envelope = objectMapper.readTree(record.value());
        String eventId = envelope.path("eventId").asText(null);
        if (eventId == null || eventId.isBlank()) {
            log.warn("audit eventId 누락으로 skip topic={} key={}", record.topic(), record.key());
            return;
        }
        Map<String, Object> payload = readPayload(envelope.path("payload"));
        AuditLog auditLog = new AuditLog(
                eventId,
                envelope.path("eventType").asText(null),
                envelope.path("aggregateId").asText(null),
                envelope.path("aggregateType").asText(null),
                envelope.path("traceId").asText(null),
                record.topic(),
                payload,
                parseInstant(envelope.path("occurredAt")),
                Instant.now()
        );
        auditLogRepository.insert(auditLog);
    }

    private Map<String, Object> readPayload(JsonNode payload) {
        if (payload.isMissingNode() || payload.isNull()) {
            return Map.of();
        }
        return objectMapper.convertValue(payload, PAYLOAD_TYPE);
    }

    private Instant parseInstant(JsonNode node) {
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return Instant.parse(node.asText());
    }
}
