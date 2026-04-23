package com.minupay.trade.order.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minupay.trade.account.application.AccountLookup;
import com.minupay.trade.account.application.AccountService;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.outbox.Outbox;
import com.minupay.trade.common.outbox.OutboxRepository;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.holding.application.HoldingService;
import com.minupay.trade.order.application.dto.CancelOrderCommand;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingProcessorTest {

    @Mock OrderRepository orderRepository;
    @Mock ExecutionRepository executionRepository;
    @Mock OutboxRepository outboxRepository;
    @Mock AccountLookup accountLookup;
    @Mock AccountService accountService;
    @Mock HoldingService holdingService;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    MatchingProcessor processor;

    @BeforeEach
    void setup() {
        processor = new MatchingProcessor(orderRepository, executionRepository, outboxRepository,
                accountLookup, accountService, holdingService, objectMapper);
        lenient().when(accountLookup.getUserId(any())).thenReturn(777L);
    }

    private Order acceptedOrder(Long id, OrderSide side, int qty) {
        Order order = Order.place(1L, "005930", side, OrderType.LIMIT,
                new BigDecimal("70000"), qty, "idem-" + id);
        order.accept();
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

    @Test
    void 매수_취소_시_잔여금액_releaseReserve_및_OrderCancelled_발행() {
        OrderBook book = new OrderBook("005930");
        Order buyer = acceptedOrder(10L, OrderSide.BUY, 10);
        book.match(buyer.getId(), OrderSide.BUY, new BigDecimal("70000"), 10);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(buyer));

        processor.cancel(book, new CancelOrderCommand(10L, "005930", 777L));

        verify(accountService).releaseReserve(eq(777L), eq(new BigDecimal("700000")));
        verify(holdingService, never()).releaseSell(any(), any(), any(Integer.class));

        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getTopic()).isEqualTo(KafkaConfig.TOPIC_ORDER_CANCELLED);
        assertThat(buyer.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(book.depth(OrderSide.BUY)).isZero();
    }

    @Test
    void 매도_취소_시_보유주식_releaseSell_호출() {
        OrderBook book = new OrderBook("005930");
        Order seller = acceptedOrder(20L, OrderSide.SELL, 5);
        book.match(seller.getId(), OrderSide.SELL, new BigDecimal("70000"), 5);
        when(orderRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(seller));

        processor.cancel(book, new CancelOrderCommand(20L, "005930", 777L));

        verify(holdingService).releaseSell(eq(777L), eq("005930"), eq(5));
        verify(accountService, never()).releaseReserve(any(), any());
        assertThat(seller.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(book.depth(OrderSide.SELL)).isZero();
    }

    @Test
    void 부분체결후_취소는_미체결수량만_release() {
        OrderBook book = new OrderBook("005930");
        Order buyer = acceptedOrder(30L, OrderSide.BUY, 10);
        buyer.addFill(3);
        book.match(buyer.getId(), OrderSide.BUY, new BigDecimal("70000"), 7);
        when(orderRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(buyer));

        processor.cancel(book, new CancelOrderCommand(30L, "005930", 777L));

        verify(accountService).releaseReserve(eq(777L), eq(new BigDecimal("490000")));
        assertThat(buyer.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void 다른_유저가_취소하면_FORBIDDEN() {
        OrderBook book = new OrderBook("005930");
        Order buyer = acceptedOrder(40L, OrderSide.BUY, 10);
        when(orderRepository.findByIdForUpdate(40L)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() ->
                processor.cancel(book, new CancelOrderCommand(40L, "005930", 999L)))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_FORBIDDEN);

        verify(accountService, never()).releaseReserve(any(), any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void 이미_체결된_주문은_취소_불가() {
        OrderBook book = new OrderBook("005930");
        Order buyer = acceptedOrder(50L, OrderSide.BUY, 10);
        buyer.addFill(10);
        when(orderRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() ->
                processor.cancel(book, new CancelOrderCommand(50L, "005930", 777L)))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_INVALID_STATE);
    }
}
