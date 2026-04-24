package com.minupay.trade.stock.infrastructure.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.consumer.ConsumedEventRecorder;
import com.minupay.trade.common.event.EventEnvelope;
import com.minupay.trade.stock.domain.Market;
import com.minupay.trade.stock.domain.Stock;
import com.minupay.trade.stock.domain.StockStatus;
import com.minupay.trade.stock.domain.event.StockUpdatedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockSearchIndexerTest {

    @Mock ConsumedEventRecorder consumedEventRecorder;
    @Mock StockSearchRepository stockSearchRepository;
    @Mock Acknowledgment ack;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    StockSearchIndexer indexer;

    @BeforeEach
    void setup() {
        indexer = new StockSearchIndexer(objectMapper, consumedEventRecorder, stockSearchRepository);
    }

    private ConsumerRecord<String, String> stockRecord() throws Exception {
        Stock stock = Stock.register("005930", "삼성전자", Market.KOSPI, "반도체",
                100, 500_000_000_000L, LocalDate.of(1975, 6, 11));
        StockUpdatedEvent event = StockUpdatedEvent.of(stock, "trace-1");
        String json = EventEnvelope.from(event).toJson(objectMapper);
        return new ConsumerRecord<>(KafkaConfig.TOPIC_STOCK_UPDATED, 0, 0L, "005930", json);
    }

    @Test
    void 최초_수신_시_ES_인덱싱_후_ack() throws Exception {
        when(consumedEventRecorder.markIfAbsent(anyString(), anyString(), anyString())).thenReturn(true);

        indexer.onMessage(stockRecord(), ack);

        ArgumentCaptor<StockSearchDocument> captor = ArgumentCaptor.forClass(StockSearchDocument.class);
        verify(stockSearchRepository).save(captor.capture());
        StockSearchDocument saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("005930");
        assertThat(saved.getName()).isEqualTo("삼성전자");
        assertThat(saved.getStatus()).isEqualTo(StockStatus.TRADING);
        assertThat(saved.getNameChosung()).isEqualTo("ㅅㅅㅈㅈ");
        verify(ack).acknowledge();
    }

    @Test
    void 중복_이벤트는_skip_ack만() throws Exception {
        when(consumedEventRecorder.markIfAbsent(anyString(), anyString(), anyString())).thenReturn(false);

        indexer.onMessage(stockRecord(), ack);

        verify(stockSearchRepository, never()).save(any());
        verify(ack).acknowledge();
    }

    @Test
    void ES_저장_실패_시_ack_하지않고_예외_전파() throws Exception {
        when(consumedEventRecorder.markIfAbsent(anyString(), anyString(), anyString())).thenReturn(true);
        doThrow(new RuntimeException("es down"))
                .when(stockSearchRepository).save(any(StockSearchDocument.class));

        assertThatThrownBy(() -> indexer.onMessage(stockRecord(), ack))
                .isInstanceOf(IllegalStateException.class);

        verify(ack, never()).acknowledge();
    }

    @Test
    void 잘못된_JSON_은_예외_전파_ack_안함() {
        ConsumerRecord<String, String> bad =
                new ConsumerRecord<>(KafkaConfig.TOPIC_STOCK_UPDATED, 0, 0L, "005930", "not-json");

        assertThatThrownBy(() -> indexer.onMessage(bad, ack))
                .isInstanceOf(IllegalStateException.class);

        verify(stockSearchRepository, never()).save(any());
        verify(ack, never()).acknowledge();
    }
}
