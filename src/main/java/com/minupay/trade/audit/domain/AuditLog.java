package com.minupay.trade.audit.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_logs")
@CompoundIndexes({
        @CompoundIndex(name = "idx_aggregate", def = "{'aggregateType': 1, 'aggregateId': 1, 'occurredAt': -1}"),
        @CompoundIndex(name = "idx_event_type_time", def = "{'eventType': 1, 'occurredAt': -1}")
})
public record AuditLog(
        @Id String eventId,
        String eventType,
        String aggregateId,
        String aggregateType,
        @Indexed String traceId,
        String topic,
        Map<String, Object> payload,
        Instant occurredAt,
        Instant recordedAt
) {
}
