package com.minupay.trade.holding.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.consumer.ConsumedEventRecorder;
import com.minupay.trade.common.event.DomainEvent;
import com.minupay.trade.common.event.EventEnvelope;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.common.money.Money;
import com.minupay.trade.common.outbox.Outbox;
import com.minupay.trade.common.outbox.OutboxRepository;
import com.minupay.trade.common.trace.TraceIdFilter;
import com.minupay.trade.holding.application.dto.HoldingInfo;
import com.minupay.trade.holding.domain.event.HoldingUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeExecutedConsumer {

    public static final String CONSUMER_GROUP = "trade-holding-updater";

    private final ObjectMapper objectMapper;
    private final ConsumedEventRecorder consumedEventRecorder;
    private final HoldingService holdingService;
    private final OutboxRepository outboxRepository;

    @Transactional
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

            String traceId = envelope.path("traceId").asText(null);
            bindTrace(traceId);

            JsonNode payload = envelope.path("payload");
            applyTrade(payload, traceId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Holding update failed key={} offset={}", record.key(), record.offset(), e);
            throw new IllegalStateException("Holding update failed", e);
        } finally {
            MDC.remove(TraceIdFilter.MDC_KEY);
        }
    }

    private void applyTrade(JsonNode payload, String traceId) {
        Long buyerUserId = payload.path("buyerUserId").asLong();
        Long sellerUserId = payload.path("sellerUserId").asLong();
        String stockCode = payload.path("stockCode").asText();
        int quantity = payload.path("quantity").asInt();
        Money price = Money.of(new BigDecimal(payload.path("price").asText()));

        HoldingInfo buyer = holdingService.applyBuy(buyerUserId, stockCode, quantity, price);
        publishHoldingUpdated(buyer, HoldingUpdatedEvent.Reason.BUY, traceId);

        HoldingInfo seller = holdingService.settleSell(sellerUserId, stockCode, quantity);
        publishHoldingUpdated(seller, HoldingUpdatedEvent.Reason.SELL, traceId);
    }

    private void publishHoldingUpdated(HoldingInfo info, HoldingUpdatedEvent.Reason reason, String traceId) {
        HoldingUpdatedEvent event = HoldingUpdatedEvent.of(
                info.userId(), info.stockCode(), info.quantity(), info.avgPrice(), reason, traceId);
        outboxRepository.save(Outbox.create(
                event.getAggregateId(),
                HoldingUpdatedEvent.AGGREGATE_TYPE,
                HoldingUpdatedEvent.EVENT_TYPE,
                KafkaConfig.TOPIC_HOLDING_UPDATED,
                info.stockCode(),
                toPayload(event)
        ));
    }

    private String toPayload(DomainEvent event) {
        try {
            return EventEnvelope.from(event).toJson(objectMapper);
        } catch (JsonProcessingException e) {
            throw new MinuTradeException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void bindTrace(String traceId) {
        if (traceId == null || traceId.isBlank()) return;
        MDC.put(TraceIdFilter.MDC_KEY, traceId);
    }
}
