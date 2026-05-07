package com.minupay.trade.broadcast.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.common.config.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TradeBroadcastConsumerTest {

    @Mock StompFanoutPublisher fanoutPublisher;
    @Mock Acknowledgment ack;

    ObjectMapper objectMapper = new ObjectMapper();
    TradeBroadcastConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TradeBroadcastConsumer(objectMapper, fanoutPublisher);
    }

    @Test
    void buyer_seller_양쪽으로_각각_publish() {
        String envelope = "{\"payload\":{\"buyerUserId\":10,\"sellerUserId\":20,\"executionId\":111}}";
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_TRADE_EXECUTED, 0, 0L, "005930", envelope);

        consumer.onMessage(record, ack);

        ArgumentCaptor<StompFanoutMessage> captor = ArgumentCaptor.forClass(StompFanoutMessage.class);
        verify(fanoutPublisher, times(2)).publish(captor.capture());
        List<StompFanoutMessage> sent = captor.getAllValues();
        assertThat(sent).extracting(StompFanoutMessage::userId).containsExactly(10L, 20L);
        assertThat(sent).extracting(StompFanoutMessage::destination)
                .containsOnly("/queue/trades");
        assertThat(sent).extracting(StompFanoutMessage::payload).containsOnly(envelope);
        verify(ack).acknowledge();
    }

    @Test
    void buyer만_있으면_buyer만_publish() {
        String envelope = "{\"payload\":{\"buyerUserId\":10,\"executionId\":111}}";
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_TRADE_EXECUTED, 0, 0L, "005930", envelope);

        consumer.onMessage(record, ack);

        ArgumentCaptor<StompFanoutMessage> captor = ArgumentCaptor.forClass(StompFanoutMessage.class);
        verify(fanoutPublisher, times(1)).publish(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(10L);
        verify(ack).acknowledge();
    }

    @Test
    void 잘못된_JSON은_예외_삼키고_ack() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_TRADE_EXECUTED, 0, 0L, "005930", "not-json");

        consumer.onMessage(record, ack);

        verify(fanoutPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
        verify(ack).acknowledge();
    }
}
