package com.minupay.trade.order.application;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.order.application.dto.MatchCommand;
import com.minupay.trade.order.domain.Execution;
import com.minupay.trade.order.domain.ExecutionRepository;
import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderRepository;
import com.minupay.trade.order.domain.orderbook.MatchResult;
import com.minupay.trade.order.domain.orderbook.OrderBook;
import com.minupay.trade.order.domain.orderbook.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingProcessor {

    private final OrderRepository orderRepository;
    private final ExecutionRepository executionRepository;

    @Transactional
    public MatchResult process(OrderBook book, MatchCommand cmd) {
        MatchResult result = book.match(cmd.orderId(), cmd.side(), cmd.price(), cmd.quantity());

        for (Trade t : result.trades()) {
            Order buy = orderRepository.findById(t.buyOrderId())
                    .orElseThrow(() -> new MinuTradeException(ErrorCode.ORDER_NOT_FOUND));
            Order sell = orderRepository.findById(t.sellOrderId())
                    .orElseThrow(() -> new MinuTradeException(ErrorCode.ORDER_NOT_FOUND));
            buy.addFill(t.quantity());
            sell.addFill(t.quantity());
            executionRepository.save(Execution.of(t.buyOrderId(), t.sellOrderId(),
                    cmd.stockCode(), t.price(), t.quantity()));
        }

        if (!result.trades().isEmpty()) {
            log.info("matched stock={} taker={} trades={} remaining={}",
                    cmd.stockCode(), cmd.orderId(), result.trades().size(), result.remainingQuantity());
        }
        return result;
    }
}
