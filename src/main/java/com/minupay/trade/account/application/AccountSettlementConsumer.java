package com.minupay.trade.account.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.consumer.ConsumedEventRecorder;
import com.minupay.trade.common.trace.TraceIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountSettlementConsumer {

    public static final String CONSUMER_GROUP = "trade-account-settler";

    private final ObjectMapper objectMapper;
    private final ConsumedEventRecorder consumedEventRecorder;
    private final AccountService accountService;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_TRADE_EXECUTED,
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

            bindTrace(envelope.path("traceId").asText(null));
            settle(envelope.path("payload"));
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Account settlement failed key={} offset={}", record.key(), record.offset(), e);
            throw new IllegalStateException("Account settlement failed", e);
        } finally {
            MDC.remove(TraceIdFilter.MDC_KEY);
        }
    }

    private void settle(JsonNode payload) {
        Long buyerUserId = payload.path("buyerUserId").asLong();
        Long sellerUserId = payload.path("sellerUserId").asLong();
        int quantity = payload.path("quantity").asInt();
        BigDecimal price = new BigDecimal(payload.path("price").asText());
        BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity));

        accountService.settleBuy(buyerUserId, amount);
        accountService.settleSell(sellerUserId, amount);
    }

    private void bindTrace(String traceId) {
        if (traceId == null || traceId.isBlank()) return;
        MDC.put(TraceIdFilter.MDC_KEY, traceId);
    }
}
