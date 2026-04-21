package com.minupay.trade.order.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.outbox.Outbox;
import com.minupay.trade.common.outbox.OutboxRepository;
import com.minupay.trade.order.application.dto.MatchCommand;
import com.minupay.trade.order.domain.Execution;
import com.minupay.trade.order.domain.ExecutionRepository;
import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderRepository;
import com.minupay.trade.order.domain.OrderSide;
import com.minupay.trade.order.domain.OrderStatus;
import com.minupay.trade.order.domain.OrderType;
import com.minupay.trade.order.domain.orderbook.MatchResult;
import com.minupay.trade.order.domain.orderbook.OrderBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingProcessorTest {

    @Mock OrderRepository orderRepository;
    @Mock ExecutionRepository executionRepository;
    @Mock OutboxRepository outboxRepository;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    MatchingProcessor processor;

    @BeforeEach
    void setup() {
        processor = new MatchingProcessor(orderRepository, executionRepository, outboxRepository, objectMapper);
    }

    private Order acceptedOrder(Long id, OrderSide side, int qty) {
        Order order = Order.place(1L, "005930", side, OrderType.LIMIT,
                new BigDecimal("70000"), qty, "idem-" + id);
        order.accept(99L);
        try {
            var f = Order.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(order, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return order;
    }

    @Test
    void 체결시_Execution_저장_Outbox_발행() {
        OrderBook book = new OrderBook("005930");
        Order seller = acceptedOrder(1L, OrderSide.SELL, 10);
        book.match(seller.getId(), OrderSide.SELL, new BigDecimal("70000"), 10);

        Order buyer = acceptedOrder(2L, OrderSide.BUY, 10);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(executionRepository.save(any(Execution.class))).thenAnswer(inv -> {
            Execution e = inv.getArgument(0);
            var f = Execution.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(e, 111L);
            return e;
        });

        MatchCommand cmd = new MatchCommand(2L, "005930", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("70000"), 10);

        MatchResult result = processor.process(book, cmd);

        assertThat(result.trades()).hasSize(1);
        verify(executionRepository, times(1)).save(any(Execution.class));

        ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository, times(3)).save(outboxCaptor.capture());

        List<Outbox> saved = outboxCaptor.getAllValues();
        assertThat(saved).extracting(Outbox::getTopic)
                .containsExactlyInAnyOrder(
                        KafkaConfig.TOPIC_TRADE_EXECUTED,
                        KafkaConfig.TOPIC_ORDER_FILLED,
                        KafkaConfig.TOPIC_ORDER_FILLED
                );
        assertThat(buyer.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(seller.getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void 부분체결은_OrderFilled_미발행() {
        OrderBook book = new OrderBook("005930");
        Order seller = acceptedOrder(1L, OrderSide.SELL, 4);
        book.match(seller.getId(), OrderSide.SELL, new BigDecimal("70000"), 4);

        Order buyer = acceptedOrder(2L, OrderSide.BUY, 10);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(buyer));
        when(executionRepository.save(any(Execution.class))).thenAnswer(inv -> inv.getArgument(0));

        MatchCommand cmd = new MatchCommand(2L, "005930", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("70000"), 10);

        processor.process(book, cmd);

        ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository, times(2)).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getAllValues()).extracting(Outbox::getTopic)
                .containsExactlyInAnyOrder(
                        KafkaConfig.TOPIC_TRADE_EXECUTED,
                        KafkaConfig.TOPIC_ORDER_FILLED
                );
        assertThat(buyer.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(seller.getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void 체결없으면_아무것도_저장안함() {
        OrderBook book = new OrderBook("005930");

        MatchCommand cmd = new MatchCommand(1L, "005930", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("70000"), 10);

        MatchResult result = processor.process(book, cmd);

        assertThat(result.trades()).isEmpty();
        verify(executionRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }
}
