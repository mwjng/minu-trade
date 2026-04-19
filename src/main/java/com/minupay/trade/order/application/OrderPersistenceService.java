package com.minupay.trade.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.event.EventEnvelope;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.common.outbox.Outbox;
import com.minupay.trade.common.outbox.OutboxRepository;
import com.minupay.trade.common.trace.TraceIdFilter;
import com.minupay.trade.order.application.dto.OrderInfo;
import com.minupay.trade.order.application.dto.PlaceOrderCommand;
import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderRepository;
import com.minupay.trade.order.domain.event.OrderAcceptedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderInfo persistAccepted(Long accountId, PlaceOrderCommand cmd, Long paymentId) {
        Order order = Order.place(
                accountId,
                cmd.stockCode(),
                cmd.side(),
                cmd.type(),
                cmd.price(),
                cmd.quantity(),
                cmd.idempotencyKey()
        );
        order.accept(paymentId);
        Order saved = orderRepository.save(order);

        publishAccepted(saved);
        return OrderInfo.from(saved);
    }

    private void publishAccepted(Order order) {
        OrderAcceptedEvent event = OrderAcceptedEvent.of(order, MDC.get(TraceIdFilter.MDC_KEY));
        String payload;
        try {
            payload = EventEnvelope.from(event).toJson(objectMapper);
        } catch (JsonProcessingException e) {
            throw new MinuTradeException(ErrorCode.INTERNAL_ERROR);
        }
        outboxRepository.save(Outbox.create(
                String.valueOf(order.getId()),
                OrderAcceptedEvent.AGGREGATE_TYPE,
                OrderAcceptedEvent.EVENT_TYPE,
                KafkaConfig.TOPIC_ORDER_ACCEPTED,
                order.getStockCode(),
                payload
        ));
    }
}
