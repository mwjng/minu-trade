package com.minupay.trade.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.account.domain.Account;
import com.minupay.trade.account.domain.AccountRepository;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.event.EventEnvelope;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.common.idempotency.IdempotencyService;
import com.minupay.trade.common.outbox.Outbox;
import com.minupay.trade.common.outbox.OutboxRepository;
import com.minupay.trade.common.trace.TraceIdFilter;
import com.minupay.trade.holding.application.HoldingService;
import com.minupay.trade.order.application.dto.OrderInfo;
import com.minupay.trade.order.application.dto.PlaceOrderCommand;
import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderRepository;
import com.minupay.trade.order.domain.OrderSide;
import com.minupay.trade.order.domain.event.OrderAcceptedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderPersistenceService {

    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final IdempotencyService idempotencyService;
    private final HoldingService holdingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderInfo persistAccepted(Long userId, PlaceOrderCommand cmd) {
        Account account = accountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.ensureCanPlaceOrder();

        reserveResources(account, userId, cmd);

        Order order = Order.place(
                account.getId(),
                cmd.stockCode(),
                cmd.side(),
                cmd.type(),
                cmd.price(),
                cmd.quantity(),
                cmd.idempotencyKey()
        );
        order.accept();
        Order saved = orderRepository.save(order);

        publishAccepted(saved);
        OrderInfo info = OrderInfo.from(saved);
        completeSlot(cmd.idempotencyKey(), info);
        return info;
    }

    private void reserveResources(Account account, Long userId, PlaceOrderCommand cmd) {
        if (cmd.side() == OrderSide.BUY) {
            BigDecimal amount = cmd.price().multiply(BigDecimal.valueOf(cmd.quantity()));
            account.reserve(amount);
            return;
        }
        holdingService.reserveSell(userId, cmd.stockCode(), cmd.quantity());
    }

    private void completeSlot(String idempotencyKey, OrderInfo info) {
        try {
            idempotencyService.complete(idempotencyKey, objectMapper.writeValueAsString(info));
        } catch (JsonProcessingException e) {
            throw new MinuTradeException(ErrorCode.INTERNAL_ERROR);
        }
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
