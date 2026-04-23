package com.minupay.trade.order.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.account.application.AccountService;
import com.minupay.trade.account.application.dto.AccountForOrder;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.common.idempotency.IdempotencyKey;
import com.minupay.trade.common.idempotency.IdempotencyService;
import com.minupay.trade.common.idempotency.IdempotencyStatus;
import com.minupay.trade.order.application.dto.OrderInfo;
import com.minupay.trade.order.application.dto.PlaceOrderCommand;
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
                orderRepository, orderPersistenceService, matchingEngine,
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
                99L, "005930", OrderSide.BUY, OrderType.LIMIT, new BigDecimal("70000"), 1, "other-key"
        );
        other.accept();
        when(orderRepository.findById(7L)).thenReturn(java.util.Optional.of(other));

        assertThatThrownBy(() -> facade.getForUser(USER_ID, 7L))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_FORBIDDEN);
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
