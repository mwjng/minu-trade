package com.minupay.trade.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.account.application.AccountLookup;
import com.minupay.trade.account.application.AccountService;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.event.DomainEvent;
import com.minupay.trade.common.event.EventEnvelope;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.common.money.Money;
import com.minupay.trade.common.outbox.Outbox;
import com.minupay.trade.common.outbox.OutboxRepository;
import com.minupay.trade.common.trace.TraceIdFilter;
import com.minupay.trade.holding.application.HoldingService;
import com.minupay.trade.order.application.dto.CancelOrderCommand;
import com.minupay.trade.order.application.dto.MatchCommand;
import com.minupay.trade.order.domain.Execution;
import com.minupay.trade.order.domain.ExecutionRepository;
import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderRepository;
import com.minupay.trade.order.domain.OrderSide;
import com.minupay.trade.order.domain.OrderStatus;
import com.minupay.trade.order.application.dto.OrderInfo;
import com.minupay.trade.order.domain.event.OrderCancelledEvent;
import com.minupay.trade.order.domain.event.OrderFilledEvent;
import com.minupay.trade.order.domain.event.TradeExecutedEvent;
import com.minupay.trade.order.domain.orderbook.MatchResult;
import com.minupay.trade.order.domain.orderbook.OrderBook;
import com.minupay.trade.order.domain.orderbook.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingProcessor {

    private final OrderRepository orderRepository;
    private final ExecutionRepository executionRepository;
    private final OutboxRepository outboxRepository;
    private final AccountLookup accountLookup;
    private final AccountService accountService;
    private final HoldingService holdingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public MatchResult process(OrderBook book, MatchCommand cmd) {
        MatchResult result = book.match(cmd.orderId(), cmd.side(), cmd.price(), cmd.quantity());
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);

        for (Trade t : result.trades()) {
            Order buy = loadOrder(t.buyOrderId());
            Order sell = loadOrder(t.sellOrderId());
            buy.addFill(t.quantity());
            sell.addFill(t.quantity());

            Execution execution = executionRepository.save(Execution.of(
                    t.buyOrderId(), t.sellOrderId(), cmd.stockCode(), t.price(), t.quantity()));

            Long buyerUserId = accountLookup.getUserId(buy.getAccountId());
            Long sellerUserId = accountLookup.getUserId(sell.getAccountId());

            publishTradeExecuted(execution, buyerUserId, sellerUserId, cmd.stockCode(), traceId);
            if (buy.getStatus() == OrderStatus.FILLED) publishOrderFilled(buy, traceId);
            if (sell.getStatus() == OrderStatus.FILLED) publishOrderFilled(sell, traceId);
        }

        if (!result.trades().isEmpty()) {
            log.info("matched stock={} taker={} trades={} remaining={}",
                    cmd.stockCode(), cmd.orderId(), result.trades().size(), result.remainingQuantity());
        }
        return result;
    }

    @Transactional
    public OrderInfo cancel(OrderBook book, CancelOrderCommand cmd) {
        Order order = orderRepository.findByIdForUpdate(cmd.orderId())
                .orElseThrow(() -> new MinuTradeException(ErrorCode.ORDER_NOT_FOUND));

        Long ownerUserId = accountLookup.getUserId(order.getAccountId());
        if (!ownerUserId.equals(cmd.userId())) {
            throw new MinuTradeException(ErrorCode.ORDER_FORBIDDEN);
        }
        if (!isCancellable(order.getStatus())) {
            throw new MinuTradeException(ErrorCode.ORDER_INVALID_STATE);
        }

        int cancelled = order.remainingQuantity();
        book.cancel(order.getId());
        releaseReserves(order, ownerUserId, cancelled);
        order.cancel();

        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        publishOrderCancelled(order, cancelled, traceId);
        log.info("order cancelled id={} stock={} cancelledQty={}", order.getId(), order.getStockCode(), cancelled);
        return OrderInfo.from(order);
    }

    private boolean isCancellable(OrderStatus status) {
        return status == OrderStatus.ACCEPTED || status == OrderStatus.PARTIALLY_FILLED;
    }

    private void releaseReserves(Order order, Long userId, int cancelledQty) {
        if (cancelledQty <= 0) {
            return;
        }
        if (order.getSide() == OrderSide.BUY) {
            Money amount = order.getPrice().multiply(cancelledQty);
            accountService.releaseReserve(userId, amount);
            return;
        }
        holdingService.releaseSell(userId, order.getStockCode(), cancelledQty);
    }

    private Order loadOrder(long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void publishTradeExecuted(Execution execution, Long buyerUserId, Long sellerUserId,
                                      String stockCode, String traceId) {
        TradeExecutedEvent event = TradeExecutedEvent.of(execution, buyerUserId, sellerUserId, traceId);
        outboxRepository.save(Outbox.create(
                String.valueOf(execution.getId()),
                TradeExecutedEvent.AGGREGATE_TYPE,
                TradeExecutedEvent.EVENT_TYPE,
                KafkaConfig.TOPIC_TRADE_EXECUTED,
                stockCode,
                toPayload(event)
        ));
    }

    private void publishOrderFilled(Order order, String traceId) {
        OrderFilledEvent event = OrderFilledEvent.of(order, traceId);
        outboxRepository.save(Outbox.create(
                String.valueOf(order.getId()),
                OrderFilledEvent.AGGREGATE_TYPE,
                OrderFilledEvent.EVENT_TYPE,
                KafkaConfig.TOPIC_ORDER_FILLED,
                order.getStockCode(),
                toPayload(event)
        ));
    }

    private void publishOrderCancelled(Order order, int cancelledQuantity, String traceId) {
        OrderCancelledEvent event = OrderCancelledEvent.of(order, cancelledQuantity, traceId);
        outboxRepository.save(Outbox.create(
                String.valueOf(order.getId()),
                OrderCancelledEvent.AGGREGATE_TYPE,
                OrderCancelledEvent.EVENT_TYPE,
                KafkaConfig.TOPIC_ORDER_CANCELLED,
                order.getStockCode(),
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
}
