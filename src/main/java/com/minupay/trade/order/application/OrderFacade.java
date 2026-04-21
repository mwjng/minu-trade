package com.minupay.trade.order.application;

import com.minupay.trade.account.application.AccountService;
import com.minupay.trade.account.application.dto.AccountForOrder;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.order.application.dto.MatchCommand;
import com.minupay.trade.order.application.dto.OrderInfo;
import com.minupay.trade.order.application.dto.PlaceOrderCommand;
import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderRepository;
import com.minupay.trade.order.domain.OrderSide;
import com.minupay.trade.order.domain.OrderType;
import com.minupay.trade.paymentclient.PayServiceClient;
import com.minupay.trade.paymentclient.dto.ChargeRequest;
import com.minupay.trade.paymentclient.dto.ChargeResponse;
import com.minupay.trade.stock.application.StockService;
import com.minupay.trade.stock.application.dto.StockInfo;
import com.minupay.trade.stock.domain.StockStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final AccountService accountService;
    private final StockService stockService;
    private final PayServiceClient payServiceClient;
    private final OrderRepository orderRepository;
    private final OrderPersistenceService orderPersistenceService;
    private final MatchingEngine matchingEngine;

    public OrderInfo placeOrder(Long userId, PlaceOrderCommand cmd) {
        Optional<Order> replay = orderRepository.findByIdempotencyKey(cmd.idempotencyKey());
        if (replay.isPresent()) {
            return OrderInfo.from(replay.get());
        }

        if (cmd.type() == OrderType.MARKET) {
            throw new MinuTradeException(ErrorCode.ORDER_MARKET_NOT_SUPPORTED);
        }

        AccountForOrder account = accountService.resolveForOrder(userId);
        StockInfo stock = stockService.getByCode(cmd.stockCode());
        ensureTradable(stock);
        ensureTickAligned(cmd.price(), stock.tickSize());

        Long paymentId = null;
        if (cmd.side() == OrderSide.BUY) {
            BigDecimal amount = cmd.price().multiply(BigDecimal.valueOf(cmd.quantity()));
            ChargeResponse resp = payServiceClient.charge(new ChargeRequest(
                    userId,
                    account.walletId(),
                    amount,
                    "BUY:" + cmd.stockCode(),
                    cmd.idempotencyKey()
            ));
            paymentId = resp.paymentId();
        }

        OrderInfo info = orderPersistenceService.persistAccepted(account.accountId(), cmd, paymentId);
        matchingEngine.submit(new MatchCommand(
                info.id(), info.stockCode(), info.side(), info.type(), info.price(), info.quantity()
        ));
        return info;
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
