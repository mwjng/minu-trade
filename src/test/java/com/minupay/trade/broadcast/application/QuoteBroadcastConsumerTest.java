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
class QuoteBroadcastConsumerTest {

    @Mock StompFanoutPublisher fanoutPublisher;
    @Mock Acknowledgment ack;

    ObjectMapper objectMapper = new ObjectMapper();
    QuoteBroadcastConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new QuoteBroadcastConsumer(objectMapper, fanoutPublisher);
    }

    @Test
    void key가_있으면_그대로_사용해서_topic_destination_생성() {
        String envelope = "{\"payload\":{\"price\":1000}}";
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_QUOTE_UPDATED, 0, 0L, "005930", envelope);

        consumer.onMessage(record, ack);

        ArgumentCaptor<StompFanoutMessage> captor = ArgumentCaptor.forClass(StompFanoutMessage.class);
        verify(fanoutPublisher).publish(captor.capture());
        assertThat(captor.getValue().destination()).isEqualTo("/topic/quotes/005930");
        assertThat(captor.getValue().userId()).isNull();
        assertThat(captor.getValue().payload()).isEqualTo(envelope);
        verify(ack).acknowledge();
    }

    @Test
    void key가_비어있으면_payload의_stockCode_사용() {
        String envelope = "{\"payload\":{\"stockCode\":\"000660\",\"price\":1000}}";
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_QUOTE_UPDATED, 0, 0L, null, envelope);

        consumer.onMessage(record, ack);

        ArgumentCaptor<StompFanoutMessage> captor = ArgumentCaptor.forClass(StompFanoutMessage.class);
        verify(fanoutPublisher).publish(captor.capture());
        assertThat(captor.getValue().destination()).isEqualTo("/topic/quotes/000660");
        verify(ack).acknowledge();
    }

    @Test
    void stockCode가_전혀_없으면_skip하고_ack() {
        String envelope = "{\"payload\":{\"price\":1000}}";
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_QUOTE_UPDATED, 0, 0L, null, envelope);

        consumer.onMessage(record, ack);

        verify(fanoutPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
        verify(ack).acknowledge();
    }

    @Test
    void 잘못된_JSON은_예외_삼키고_ack() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_QUOTE_UPDATED, 0, 0L, null, "not-json");

        consumer.onMessage(record, ack);

        verify(fanoutPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
        verify(ack).acknowledge();
    }
}
