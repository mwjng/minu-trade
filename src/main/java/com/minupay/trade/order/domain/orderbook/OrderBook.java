package com.minupay.trade.order.domain.orderbook;

import com.minupay.trade.common.money.Money;
import com.minupay.trade.order.domain.OrderSide;
import lombok.Getter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public class OrderBook {

    @Getter
    private final String stockCode;

    private final NavigableMap<Money, Deque<BookEntry>> buys = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<Money, Deque<BookEntry>> sells = new TreeMap<>();
    private final Map<Long, BookEntry> index = new HashMap<>();
    private long sequence = 0L;

    public OrderBook(String stockCode) {
        this.stockCode = stockCode;
    }

    public MatchResult match(long orderId, OrderSide side, Money price, int quantity) {
        List<Trade> trades = new ArrayList<>();
        int remaining = quantity;
        NavigableMap<Money, Deque<BookEntry>> opposite = (side == OrderSide.BUY) ? sells : buys;

        while (remaining > 0 && !opposite.isEmpty()) {
            Money bestPrice = opposite.firstKey();
            if (!crosses(side, price, bestPrice)) break;

            Deque<BookEntry> queue = opposite.get(bestPrice);
            BookEntry maker = queue.peekFirst();
            int fill = Math.min(remaining, maker.getRemainingQuantity());

            long buyId = (side == OrderSide.BUY) ? orderId : maker.getOrderId();
            long sellId = (side == OrderSide.BUY) ? maker.getOrderId() : orderId;
            trades.add(new Trade(buyId, sellId, bestPrice, fill));

            maker.consume(fill);
            remaining -= fill;

            if (maker.isEmpty()) {
                queue.pollFirst();
                index.remove(maker.getOrderId());
                if (queue.isEmpty()) opposite.remove(bestPrice);
            }
        }

        boolean added = false;
        if (remaining > 0 && price != null) {
            BookEntry entry = new BookEntry(orderId, side, price, remaining, ++sequence);
            NavigableMap<Money, Deque<BookEntry>> own = (side == OrderSide.BUY) ? buys : sells;
            own.computeIfAbsent(price, p -> new ArrayDeque<>()).addLast(entry);
            index.put(orderId, entry);
            added = true;
        }
        return new MatchResult(List.copyOf(trades), remaining, added);
    }

    public boolean cancel(long orderId) {
        BookEntry entry = index.remove(orderId);
        if (entry == null) return false;
        NavigableMap<Money, Deque<BookEntry>> side = (entry.getSide() == OrderSide.BUY) ? buys : sells;
        Deque<BookEntry> queue = side.get(entry.getPrice());
        if (queue == null) return false;
        queue.remove(entry);
        if (queue.isEmpty()) side.remove(entry.getPrice());
        return true;
    }

    public Optional<Money> bestBid() {
        return buys.isEmpty() ? Optional.empty() : Optional.of(buys.firstKey());
    }

    public Optional<Money> bestAsk() {
        return sells.isEmpty() ? Optional.empty() : Optional.of(sells.firstKey());
    }

    public int depth(OrderSide side) {
        NavigableMap<Money, Deque<BookEntry>> map = (side == OrderSide.BUY) ? buys : sells;
        return map.values().stream().mapToInt(Deque::size).sum();
    }

    private boolean crosses(OrderSide taker, Money takerPrice, Money makerPrice) {
        if (takerPrice == null) return true;
        return (taker == OrderSide.BUY)
                ? takerPrice.compareTo(makerPrice) >= 0
                : makerPrice.compareTo(takerPrice) >= 0;
    }
}
