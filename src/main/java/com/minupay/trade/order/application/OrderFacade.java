package com.minupay.trade.order.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.account.application.AccountService;
import com.minupay.trade.account.application.dto.AccountForOrder;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.common.idempotency.IdempotencyKey;
import com.minupay.trade.common.idempotency.IdempotencyService;
import com.minupay.trade.common.money.Money;
import com.minupay.trade.order.application.dto.CancelOrderCommand;
import com.minupay.trade.order.application.dto.MatchCommand;
import com.minupay.trade.order.application.dto.OrderInfo;
import com.minupay.trade.order.application.dto.PlaceOrderCommand;
import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderRepository;
import com.minupay.trade.order.domain.OrderStatus;
import com.minupay.trade.order.domain.OrderType;
import com.minupay.trade.stock.application.StockService;
import com.minupay.trade.stock.application.dto.StockInfo;
import com.minupay.trade.stock.domain.StockStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final AccountService accountService;
    private final StockService stockService;
    private final OrderRepository orderRepository;
    private final OrderPersistenceService orderPersistenceService;
    private final MatchingEngine matchingEngine;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public OrderInfo placeOrder(Long userId, PlaceOrderCommand cmd) {
        if (cmd.type() == OrderType.MARKET) {
            throw new MinuTradeException(ErrorCode.ORDER_MARKET_NOT_SUPPORTED);
        }

        String requestHash = hashRequest(userId, cmd);

        try {
            idempotencyService.acquireSlot(cmd.idempotencyKey(), requestHash, IdempotencyService.DEFAULT_TTL);
        } catch (DataIntegrityViolationException e) {
            return resolveReplay(cmd.idempotencyKey(), requestHash);
        }

        try {
            StockInfo stock = stockService.getByCode(cmd.stockCode());
            ensureTradable(stock);
            ensureTickAligned(cmd.price(), stock.tickSize());

            OrderInfo info = orderPersistenceService.persistAccepted(userId, cmd);
            Money matchPrice = info.price() == null ? null : Money.of(info.price());
            matchingEngine.submit(new MatchCommand(
                    info.id(), info.stockCode(), info.side(), info.type(), matchPrice, info.quantity()
            ));
            return info;
        } catch (RuntimeException ex) {
            idempotencyService.fail(cmd.idempotencyKey());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public OrderInfo getForUser(Long userId, Long orderId) {
        AccountForOrder account = accountService.resolveForOrder(userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getAccountId().equals(account.accountId())) {
            throw new MinuTradeException(ErrorCode.ORDER_FORBIDDEN);
        }
        return OrderInfo.from(order);
    }

    public OrderInfo cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.ORDER_NOT_FOUND));
        AccountForOrder account = accountService.resolveForOrder(userId);
        if (!order.getAccountId().equals(account.accountId())) {
            throw new MinuTradeException(ErrorCode.ORDER_FORBIDDEN);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return OrderInfo.from(order);
        }

        CancelOrderCommand cmd = new CancelOrderCommand(orderId, order.getStockCode(), userId);
        try {
            return matchingEngine.cancel(cmd).get(CANCEL_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (ExecutionException | CompletionException e) {
            if (e.getCause() instanceof MinuTradeException mte) throw mte;
            throw new MinuTradeException(ErrorCode.INTERNAL_ERROR);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new MinuTradeException(ErrorCode.INTERNAL_ERROR);
        } catch (TimeoutException te) {
            throw new MinuTradeException(ErrorCode.ORDER_CANCEL_TIMEOUT);
        }
    }

    private static final long CANCEL_TIMEOUT_SEC = 3;

    private OrderInfo resolveReplay(String key, String requestHash) {
        IdempotencyKey slot = idempotencyService.getSlot(key);
        if (!slot.matchesRequest(requestHash)) {
            throw new MinuTradeException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
        return switch (slot.getStatus()) {
            case COMPLETED -> deserialize(slot.getResponse());
            case IN_PROGRESS -> throw new MinuTradeException(ErrorCode.IDEMPOTENCY_IN_PROGRESS);
            case FAILED -> throw new MinuTradeException(ErrorCode.DUPLICATE_REQUEST);
        };
    }

    private OrderInfo deserialize(String json) {
        try {
            return objectMapper.readValue(json, OrderInfo.class);
        } catch (JsonProcessingException e) {
            throw new MinuTradeException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String hashRequest(Long userId, PlaceOrderCommand cmd) {
        String raw = userId + "|" + cmd.stockCode() + "|" + cmd.side() + "|" + cmd.type()
                + "|" + (cmd.price() == null ? "" : cmd.price().toPlainString())
                + "|" + cmd.quantity();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new MinuTradeException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void ensureTradable(StockInfo stock) {
        if (stock.status() != StockStatus.TRADING) {
            throw new MinuTradeException(ErrorCode.STOCK_NOT_TRADABLE);
        }
    }

    private void ensureTickAligned(BigDecimal price, int tickSize) {
        if (price == null) return;
        BigDecimal tick = BigDecimal.valueOf(tickSize);
        if (price.remainder(tick).signum() != 0) {
            throw new MinuTradeException(ErrorCode.ORDER_TICK_NOT_ALIGNED);
        }
    }
}
