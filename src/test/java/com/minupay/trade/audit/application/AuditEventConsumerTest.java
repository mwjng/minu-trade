package com.minupay.trade.audit.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.audit.domain.AuditLog;
import com.minupay.trade.audit.infrastructure.AuditLogRepository;
import com.minupay.trade.common.config.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock Acknowledgment ack;

    ObjectMapper objectMapper = new ObjectMapper();
    AuditEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuditEventConsumer(objectMapper, auditLogRepository);
    }

    @Test
    void envelope을_그대로_AuditLog로_변환해서_저장() {
        String envelope = """
                {
                  "eventId": "evt-1",
                  "eventType": "order.filled",
                  "aggregateId": "100",
                  "aggregateType": "Order",
                  "traceId": "trace-abc",
                  "occurredAt": "2026-05-15T12:34:56Z",
                  "payload": { "orderId": 100, "userId": 7, "stockCode": "005930" }
                }
                """;
        ConsumerRecord<String, String> record = recordOf(KafkaConfig.TOPIC_ORDER_FILLED, envelope);

        consumer.onMessage(record, ack);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).insert(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.eventId()).isEqualTo("evt-1");
        assertThat(saved.eventType()).isEqualTo("order.filled");
        assertThat(saved.aggregateId()).isEqualTo("100");
        assertThat(saved.aggregateType()).isEqualTo("Order");
        assertThat(saved.traceId()).isEqualTo("trace-abc");
        assertThat(saved.topic()).isEqualTo(KafkaConfig.TOPIC_ORDER_FILLED);
        assertThat(saved.occurredAt()).isEqualTo(Instant.parse("2026-05-15T12:34:56Z"));
        assertThat(saved.recordedAt()).isNotNull();
        assertThat(saved.payload())
                .containsEntry("orderId", 100)
                .containsEntry("userId", 7)
                .containsEntry("stockCode", "005930");
        verify(ack).acknowledge();
    }

    @Test
    void 중복_이벤트는_DuplicateKey_삼키고_ack() {
        String envelope = """
                {"eventId":"evt-1","eventType":"order.accepted","occurredAt":"2026-05-15T00:00:00Z","payload":{}}
                """;
        when(auditLogRepository.insert(any(AuditLog.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        consumer.onMessage(recordOf(KafkaConfig.TOPIC_ORDER_ACCEPTED, envelope), ack);

        verify(ack).acknowledge();
    }

    @Test
    void eventId_없으면_저장하지_않고_ack() {
        String envelope = "{\"eventType\":\"order.accepted\",\"payload\":{}}";

        consumer.onMessage(recordOf(KafkaConfig.TOPIC_ORDER_ACCEPTED, envelope), ack);

        verify(auditLogRepository, never()).insert(any(AuditLog.class));
        verify(ack).acknowledge();
    }

    @Test
    void 잘못된_JSON은_예외_삼키고_ack() {
        consumer.onMessage(recordOf(KafkaConfig.TOPIC_ORDER_ACCEPTED, "not-json"), ack);

        verify(auditLogRepository, never()).insert(any(AuditLog.class));
        verify(ack).acknowledge();
    }

    @Test
    void payload_없는_envelope도_빈_Map으로_저장() {
        String envelope = """
                {"eventId":"evt-2","eventType":"stock.updated","occurredAt":"2026-05-15T00:00:00Z"}
                """;

        consumer.onMessage(recordOf(KafkaConfig.TOPIC_STOCK_UPDATED, envelope), ack);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).insert(captor.capture());
        assertThat(captor.getValue().payload()).isEmpty();
        verify(ack).acknowledge();
    }

    private ConsumerRecord<String, String> recordOf(String topic, String value) {
        return new ConsumerRecord<>(topic, 0, 0L, null, value);
    }
}
