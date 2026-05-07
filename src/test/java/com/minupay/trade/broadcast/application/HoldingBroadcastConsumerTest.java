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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HoldingBroadcastConsumerTest {

    @Mock StompFanoutPublisher fanoutPublisher;
    @Mock Acknowledgment ack;

    ObjectMapper objectMapper = new ObjectMapper();
    HoldingBroadcastConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new HoldingBroadcastConsumer(objectMapper, fanoutPublisher);
    }

    @Test
    void payload의_userId로_holdings_큐로_publish() {
        String envelope = "{\"payload\":{\"userId\":10,\"stockCode\":\"005930\",\"quantity\":5}}";
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_HOLDING_UPDATED, 0, 0L, "005930", envelope);

        consumer.onMessage(record, ack);

        ArgumentCaptor<StompFanoutMessage> captor = ArgumentCaptor.forClass(StompFanoutMessage.class);
        verify(fanoutPublisher).publish(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(10L);
        assertThat(captor.getValue().destination()).isEqualTo("/queue/holdings");
        assertThat(captor.getValue().payload()).isEqualTo(envelope);
        verify(ack).acknowledge();
    }

    @Test
    void userId_없으면_skip하고_ack() {
        String envelope = "{\"payload\":{\"stockCode\":\"005930\"}}";
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_HOLDING_UPDATED, 0, 0L, "005930", envelope);

        consumer.onMessage(record, ack);

        verify(fanoutPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
        verify(ack).acknowledge();
    }

    @Test
    void 잘못된_JSON은_예외_삼키고_ack() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_HOLDING_UPDATED, 0, 0L, "005930", "not-json");

        consumer.onMessage(record, ack);

        verify(fanoutPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
        verify(ack).acknowledge();
    }
}
