package com.minupay.trade.holding.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.consumer.ConsumedEventRecorder;
import com.minupay.trade.common.event.EventEnvelope;
import com.minupay.trade.common.outbox.Outbox;
import com.minupay.trade.common.outbox.OutboxRepository;
import com.minupay.trade.holding.application.dto.HoldingInfo;
import com.minupay.trade.holding.domain.event.HoldingUpdatedEvent;
import com.minupay.trade.order.domain.Execution;
import com.minupay.trade.order.domain.event.TradeExecutedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeExecutedConsumerTest {

    @Mock ConsumedEventRecorder consumedEventRecorder;
    @Mock HoldingService holdingService;
    @Mock OutboxRepository outboxRepository;
    @Mock Acknowledgment ack;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    TradeExecutedConsumer consumer;

    @BeforeEach
    void setup() {
        consumer = new TradeExecutedConsumer(objectMapper, consumedEventRecorder, holdingService, outboxRepository);
    }

    private ConsumerRecord<String, String> tradeRecord(Long buyerUserId, Long sellerUserId) throws Exception {
        Execution execution = executionStub();
        TradeExecutedEvent event = TradeExecutedEvent.of(execution, buyerUserId, sellerUserId, "trace-1");
        String json = EventEnvelope.from(event).toJson(objectMapper);
        return new ConsumerRecord<>(KafkaConfig.TOPIC_TRADE_EXECUTED, 0, 0L, "005930", json);
    }

    private Execution executionStub() throws Exception {
        Execution execution = Execution.of(1L, 2L, "005930", new BigDecimal("70000"), 10);
        var f = Execution.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(execution, 111L);
        return execution;
    }

    @Test
    void 최초_수신_시_buy_sell_양쪽_반영_그리고_holding_updated_2건_발행() throws Exception {
        when(consumedEventRecorder.markIfAbsent(anyString(), anyString(), anyString())).thenReturn(true);
        when(holdingService.applyBuy(eq(10L), eq("005930"), anyInt(), any()))
                .thenReturn(new HoldingInfo(1L, 10L, "005930", 10, new BigDecimal("70000")));
        when(holdingService.applySell(eq(20L), eq("005930"), anyInt()))
                .thenReturn(new HoldingInfo(2L, 20L, "005930", 0, new BigDecimal("60000")));

        consumer.onMessage(tradeRecord(10L, 20L), ack);

        verify(holdingService).applyBuy(10L, "005930", 10, new BigDecimal("70000"));
        verify(holdingService).applySell(20L, "005930", 10);

        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository, times(2)).save(captor.capture());
        List<Outbox> saved = captor.getAllValues();
        assertThat(saved).extracting(Outbox::getTopic)
                .containsOnly(KafkaConfig.TOPIC_HOLDING_UPDATED);
        assertThat(saved).extracting(Outbox::getEventType)
                .containsOnly(HoldingUpdatedEvent.EVENT_TYPE);
        verify(ack).acknowledge();
    }

    @Test
    void 중복_이벤트는_skip_ack만() throws Exception {
        when(consumedEventRecorder.markIfAbsent(anyString(), anyString(), anyString())).thenReturn(false);

        consumer.onMessage(tradeRecord(10L, 20L), ack);

        verify(holdingService, never()).applyBuy(anyLong(), anyString(), anyInt(), any());
        verify(holdingService, never()).applySell(anyLong(), anyString(), anyInt());
        verify(outboxRepository, never()).save(any());
        verify(ack).acknowledge();
    }
}
