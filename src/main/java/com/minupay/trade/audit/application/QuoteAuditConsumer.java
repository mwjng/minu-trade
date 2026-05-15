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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "audit.consumer", name = "include-quote", havingValue = "true")
public class QuoteAuditConsumer {

    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final AuditLogRepository auditLogRepository;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_QUOTE_UPDATED,
            groupId = "${audit.consumer.group-id}-quote",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            persist(record);
        } catch (DuplicateKeyException e) {
            log.debug("quote audit 중복 이벤트 skip key={}", record.key());
        } catch (Exception e) {
            log.warn("quote audit 적재 실패 key={}", record.key(), e);
        } finally {
            ack.acknowledge();
        }
    }

    private void persist(ConsumerRecord<String, String> record) throws Exception {
        JsonNode envelope = objectMapper.readTree(record.value());
        String eventId = envelope.path("eventId").asText(null);
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        AuditLog auditLog = new AuditLog(
                eventId,
                envelope.path("eventType").asText(null),
                envelope.path("aggregateId").asText(null),
                envelope.path("aggregateType").asText(null),
                envelope.path("traceId").asText(null),
                record.topic(),
                objectMapper.convertValue(envelope.path("payload"), PAYLOAD_TYPE),
                Instant.parse(envelope.path("occurredAt").asText()),
                Instant.now()
        );
        auditLogRepository.insert(auditLog);
    }
}
