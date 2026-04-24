package com.minupay.trade.order.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.account.application.AccountService;
import com.minupay.trade.account.application.dto.AccountForOrder;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.common.idempotency.IdempotencyKey;
import com.minupay.trade.common.idempotency.IdempotencyService;
import com.minupay.trade.common.idempotency.IdempotencyStatus;
import com.minupay.trade.common.money.Money;
import com.minupay.trade.order.application.dto.CancelOrderCommand;
import com.minupay.trade.order.application.dto.ExecutionInfo;
import com.minupay.trade.order.application.dto.OrderInfo;
import com.minupay.trade.order.application.dto.PlaceOrderCommand;
import com.minupay.trade.order.domain.Execution;
import com.minupay.trade.order.domain.ExecutionRepository;
import com.minupay.trade.order.domain.Order;
import com.minupay.trade.order.domain.OrderRepository;
import com.minupay.trade.order.domain.OrderSide;
import com.minupay.trade.order.domain.OrderStatus;
import com.minupay.trade.order.domain.OrderType;
import com.minupay.trade.stock.application.StockService;
import com.minupay.trade.stock.application.dto.StockInfo;
import com.minupay.trade.stock.domain.Market;
import com.minupay.trade.stock.domain.StockStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock AccountService accountService;
    @Mock StockService stockService;
    @Mock OrderRepository orderRepository;
    @Mock ExecutionRepository executionRepository;
    @Mock OrderPersistenceService orderPersistenceService;
    @Mock MatchingEngine matchingEngine;
    @Mock IdempotencyService idempotencyService;

    ObjectMapper objectMapper = new ObjectMapper();
    OrderFacade facade;

    private static final String IDEM_KEY = "idem-test-1";
    private static final Long USER_ID = 42L;

    @BeforeEach
    void setup() {
        facade = new OrderFacade(
                accountService, stockService,
                orderRepository, executionRepository, orderPersistenceService, matchingEngine,
                idempotencyService, objectMapper
        );
    }

    private PlaceOrderCommand buyCmd() {
        return new PlaceOrderCommand("005930", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("70000"), 10, IDEM_KEY);
    }

    private StockInfo tradingStock() {
        return new StockInfo("005930", "삼성전자", Market.KOSPI, "반도체",
                100, 500_000_000_000L, StockStatus.TRADING, LocalDate.of(1975, 6, 11));
    }

    private OrderInfo samplePersistedInfo() {
        return new OrderInfo(7L, 1L, "005930", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("70000"), 10, 0, OrderStatus.ACCEPTED);
    }

    @Test
    void 첫_요청이면_slot_획득_후_persistAccepted_호출() {
        when(stockService.getByCode("005930")).thenReturn(tradingStock());
        when(orderPersistenceService.persistAccepted(eq(USER_ID), any(PlaceOrderCommand.class)))
                .thenReturn(samplePersistedInfo());

        OrderInfo result = facade.placeOrder(USER_ID, buyCmd());

        assertThat(result.id()).isEqualTo(7L);
        verify(idempotencyService).acquireSlot(eq(IDEM_KEY), anyString(), any(Duration.class));
        verify(orderPersistenceService).persistAccepted(eq(USER_ID), any(PlaceOrderCommand.class));
        verify(matchingEngine).submit(any());
    }

    @Test
    void 중복_요청_COMPLETED면_저장된_응답_반환() throws Exception {
        OrderInfo stored = samplePersistedInfo();
        String json = objectMapper.writeValueAsString(stored);

        String[] hashCapture = new String[1];
        doAnswer(inv -> {
            hashCapture[0] = inv.getArgument(1);
            throw new DataIntegrityViolationException("dup");
        }).when(idempotencyService).acquireSlot(eq(IDEM_KEY), anyString(), any(Duration.class));

        IdempotencyKey completedSlot = spyIdemKey(IDEM_KEY, null, json, IdempotencyStatus.COMPLETED);
        when(idempotencyService.getSlot(IDEM_KEY)).thenAnswer(inv -> {
            var f = IdempotencyKey.class.getDeclaredField("requestHash");
            f.setAccessible(true);
            f.set(completedSlot, hashCapture[0]);
            return completedSlot;
        });

        OrderInfo result = facade.placeOrder(USER_ID, buyCmd());

        assertThat(result.id()).isEqualTo(7L);
        verify(orderPersistenceService, never()).persistAccepted(any(), any());
        verify(matchingEngine, never()).submit(any());
    }

    @Test
    void 중복_요청_IN_PROGRESS면_409_예외() {
        String[] hashCapture = new String[1];
        doAnswer(inv -> {
            hashCapture[0] = inv.getArgument(1);
            throw new DataIntegrityViolationException("dup");
        }).when(idempotencyService).acquireSlot(eq(IDEM_KEY), anyString(), any(Duration.class));

        IdempotencyKey inProgress = IdempotencyKey.startInProgress(IDEM_KEY, "placeholder",
                Instant.now().plus(Duration.ofHours(1)));
        when(idempotencyService.getSlot(IDEM_KEY)).thenAnswer(inv -> {
            var f = IdempotencyKey.class.getDeclaredField("requestHash");
            f.setAccessible(true);
            f.set(inProgress, hashCapture[0]);
            return inProgress;
        });

        assertThatThrownBy(() -> facade.placeOrder(USER_ID, buyCmd()))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.IDEMPOTENCY_IN_PROGRESS);
    }

    @Test
    void 같은키_다른요청이면_IDEMPOTENCY_CONFLICT() {
        doAnswer(inv -> { throw new DataIntegrityViolationException("dup"); })
                .when(idempotencyService).acquireSlot(eq(IDEM_KEY), anyString(), any(Duration.class));

        IdempotencyKey differentHash = IdempotencyKey.startInProgress(IDEM_KEY,
                "DIFFERENT_HASH", Instant.now().plus(Duration.ofHours(1)));
        when(idempotencyService.getSlot(IDEM_KEY)).thenReturn(differentHash);

        assertThatThrownBy(() -> facade.placeOrder(USER_ID, buyCmd()))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT);
    }

    @Test
    void 비즈니스로직_실패_시_slot_fail_호출() {
        when(stockService.getByCode("005930")).thenReturn(tradingStock());
        when(orderPersistenceService.persistAccepted(eq(USER_ID), any(PlaceOrderCommand.class)))
                .thenThrow(new MinuTradeException(ErrorCode.ACCOUNT_INSUFFICIENT_BALANCE));

        assertThatThrownBy(() -> facade.placeOrder(USER_ID, buyCmd()))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INSUFFICIENT_BALANCE);

        verify(idempotencyService).fail(IDEM_KEY);
        verify(matchingEngine, never()).submit(any());
    }

    @Test
    void MARKET_주문은_slot_취득전에_거부() {
        PlaceOrderCommand market = new PlaceOrderCommand("005930", OrderSide.BUY, OrderType.MARKET,
                null, 10, IDEM_KEY);

        assertThatThrownBy(() -> facade.placeOrder(USER_ID, market))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_MARKET_NOT_SUPPORTED);

        verify(idempotencyService, never()).acquireSlot(anyString(), anyString(), any());
    }

    @Test
    void getForUser_다른_유저_주문이면_FORBIDDEN() {
        AccountForOrder account = new AccountForOrder(1L);
        when(accountService.resolveForOrder(USER_ID)).thenReturn(account);
        com.minupay.trade.order.domain.Order other = com.minupay.trade.order.domain.Order.place(
                99L, "005930", OrderSide.BUY, OrderType.LIMIT, Money.of(new BigDecimal("70000")), 1, "other-key"
        );
        other.accept();
        when(orderRepository.findById(7L)).thenReturn(java.util.Optional.of(other));

        assertThatThrownBy(() -> facade.getForUser(USER_ID, 7L))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_FORBIDDEN);
    }

    @Test
    void 취소_요청은_matchingEngine_cancel_호출_후_결과_반환() {
        Order order = Order.place(1L, "005930", OrderSide.BUY, OrderType.LIMIT,
                Money.of(new BigDecimal("70000")), 10, "idem-c");
        order.accept();
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(accountService.resolveForOrder(USER_ID)).thenReturn(new AccountForOrder(1L));

        OrderInfo cancelled = new OrderInfo(7L, 1L, "005930", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("70000"), 10, 0, OrderStatus.CANCELLED);
        when(matchingEngine.cancel(any(CancelOrderCommand.class)))
                .thenReturn(CompletableFuture.completedFuture(cancelled));

        OrderInfo result = facade.cancelOrder(USER_ID, 7L);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(matchingEngine).cancel(any(CancelOrderCommand.class));
    }

    @Test
    void 취소_다른_유저_주문이면_FORBIDDEN_그리고_engine_미호출() {
        Order order = Order.place(99L, "005930", OrderSide.BUY, OrderType.LIMIT,
                Money.of(new BigDecimal("70000")), 10, "idem-c");
        order.accept();
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(accountService.resolveForOrder(USER_ID)).thenReturn(new AccountForOrder(1L));

        assertThatThrownBy(() -> facade.cancelOrder(USER_ID, 7L))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_FORBIDDEN);

        verify(matchingEngine, never()).cancel(any());
    }

    @Test
    void 이미_취소된_주문은_엔진_호출없이_바로_반환() {
        Order order = Order.place(1L, "005930", OrderSide.BUY, OrderType.LIMIT,
                Money.of(new BigDecimal("70000")), 10, "idem-c");
        order.accept();
        order.cancel();
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(accountService.resolveForOrder(USER_ID)).thenReturn(new AccountForOrder(1L));

        OrderInfo result = facade.cancelOrder(USER_ID, 7L);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(matchingEngine, never()).cancel(any());
    }

    @Test
    void listForUser_status_없으면_null로_레포_호출하고_DTO로_매핑() {
        AccountForOrder account = new AccountForOrder(1L);
        when(accountService.resolveForOrder(USER_ID)).thenReturn(account);
        Order order = Order.place(1L, "005930", OrderSide.BUY, OrderType.LIMIT,
                Money.of(new BigDecimal("70000")), 10, "k1");
        Pageable pageable = PageRequest.of(0, 20);
        when(orderRepository.findByAccountId(1L, null, pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        Page<OrderInfo> page = facade.listForUser(USER_ID, null, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).stockCode()).isEqualTo("005930");
        verify(orderRepository).findByAccountId(1L, null, pageable);
    }

    @Test
    void listForUser_status_주어지면_같은_레포_메서드에_status_전달() {
        AccountForOrder account = new AccountForOrder(1L);
        when(accountService.resolveForOrder(USER_ID)).thenReturn(account);
        Pageable pageable = PageRequest.of(0, 20);
        when(orderRepository.findByAccountId(1L, OrderStatus.FILLED, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<OrderInfo> page = facade.listForUser(USER_ID, OrderStatus.FILLED, pageable);

        assertThat(page.getTotalElements()).isZero();
        verify(orderRepository).findByAccountId(1L, OrderStatus.FILLED, pageable);
    }

    @Test
    void listExecutionsForUser_본인_주문이면_체결_페이지_반환() throws Exception {
        AccountForOrder account = new AccountForOrder(1L);
        when(accountService.resolveForOrder(USER_ID)).thenReturn(account);
        Order order = Order.place(1L, "005930", OrderSide.BUY, OrderType.LIMIT,
                Money.of(new BigDecimal("70000")), 10, "idem-exec");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        Execution exec = Execution.of(7L, 8L, "005930", Money.of(new BigDecimal("70000")), 5);
        Pageable pageable = PageRequest.of(0, 20);
        when(executionRepository.findByOrderId(7L, pageable))
                .thenReturn(new PageImpl<>(List.of(exec), pageable, 1));

        Page<ExecutionInfo> page = facade.listExecutionsForUser(USER_ID, 7L, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void listExecutionsForUser_다른_유저_주문이면_FORBIDDEN() {
        AccountForOrder account = new AccountForOrder(1L);
        when(accountService.resolveForOrder(USER_ID)).thenReturn(account);
        Order other = Order.place(99L, "005930", OrderSide.BUY, OrderType.LIMIT,
                Money.of(new BigDecimal("70000")), 10, "idem-x");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> facade.listExecutionsForUser(USER_ID, 7L, PageRequest.of(0, 20)))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_FORBIDDEN);

        verify(executionRepository, never()).findByOrderId(any(), any());
    }

    private IdempotencyKey spyIdemKey(String key, String hash, String response, IdempotencyStatus status) {
        IdempotencyKey slot = IdempotencyKey.startInProgress(key,
                hash == null ? "placeholder" : hash,
                Instant.now().plus(Duration.ofHours(1)));
        if (status == IdempotencyStatus.COMPLETED) {
            slot.complete(response);
        } else if (status == IdempotencyStatus.FAILED) {
            slot.fail();
        }
        return slot;
    }

    private static <T> Stubber doAnswer(Answer<T> answer) {
        return org.mockito.Mockito.doAnswer(answer);
    }
}
