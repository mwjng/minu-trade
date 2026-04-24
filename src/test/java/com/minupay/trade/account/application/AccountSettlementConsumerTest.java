package com.minupay.trade.account.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.consumer.ConsumedEventRecorder;
import com.minupay.trade.common.event.EventEnvelope;
import com.minupay.trade.common.money.Money;
import com.minupay.trade.order.domain.Execution;
import com.minupay.trade.order.domain.event.TradeExecutedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSettlementConsumerTest {

    @Mock ConsumedEventRecorder consumedEventRecorder;
    @Mock AccountService accountService;
    @Mock Acknowledgment ack;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    AccountSettlementConsumer consumer;

    @BeforeEach
    void setup() {
        consumer = new AccountSettlementConsumer(objectMapper, consumedEventRecorder, accountService);
    }

    private ConsumerRecord<String, String> tradeRecord(Long buyerUserId, Long sellerUserId,
                                                       BigDecimal price, int quantity) throws Exception {
        Execution execution = Execution.of(1L, 2L, "005930", Money.of(price), quantity);
        var f = Execution.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(execution, 111L);
        TradeExecutedEvent event = TradeExecutedEvent.of(execution, buyerUserId, sellerUserId, "trace-1");
        String json = EventEnvelope.from(event).toJson(objectMapper);
        return new ConsumerRecord<>(KafkaConfig.TOPIC_TRADE_EXECUTED, 0, 0L, "005930", json);
    }

    @Test
    void 체결_수신_시_buyer_settleBuy_seller_settleSell_호출() throws Exception {
        when(consumedEventRecorder.markIfAbsent(anyString(), anyString(), anyString())).thenReturn(true);

        consumer.onMessage(tradeRecord(10L, 20L, new BigDecimal("70000"), 3), ack);

        Money expected = Money.of(new BigDecimal("210000"));
        verify(accountService).settleBuy(eq(10L), eq(expected));
        verify(accountService).settleSell(eq(20L), eq(expected));
        verify(ack).acknowledge();
    }

    @Test
    void 중복_이벤트는_skip_ack만() throws Exception {
        when(consumedEventRecorder.markIfAbsent(anyString(), anyString(), anyString())).thenReturn(false);

        consumer.onMessage(tradeRecord(10L, 20L, new BigDecimal("70000"), 3), ack);

        verify(accountService, never()).settleBuy(anyLong(), any());
        verify(accountService, never()).settleSell(anyLong(), any());
        verify(ack).acknowledge();
    }

    @Test
    void 정산_실패_시_ack하지_않고_예외_전파() throws Exception {
        when(consumedEventRecorder.markIfAbsent(anyString(), anyString(), anyString())).thenReturn(true);
        doThrow(new RuntimeException("db down"))
                .when(accountService).settleSell(anyLong(), any());

        assertThatThrownBy(() -> consumer.onMessage(tradeRecord(10L, 20L, new BigDecimal("70000"), 3), ack))
                .isInstanceOf(IllegalStateException.class);

        verify(ack, never()).acknowledge();
    }

    @Test
    void 잘못된_JSON_은_예외_전파_ack_안함() {
        ConsumerRecord<String, String> bad =
                new ConsumerRecord<>(KafkaConfig.TOPIC_TRADE_EXECUTED, 0, 0L, "005930", "not-json");

        assertThatThrownBy(() -> consumer.onMessage(bad, ack))
                .isInstanceOf(IllegalStateException.class);

        verify(accountService, never()).settleBuy(anyLong(), any());
        verify(accountService, never()).settleSell(anyLong(), any());
        verify(ack, never()).acknowledge();
    }
}
