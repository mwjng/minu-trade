package com.minupay.trade.broadcast.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.account.application.AccountLookup;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderBroadcastConsumerTest {

    @Mock StompFanoutPublisher fanoutPublisher;
    @Mock AccountLookup accountLookup;
    @Mock Acknowledgment ack;

    ObjectMapper objectMapper = new ObjectMapper();
    OrderBroadcastConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderBroadcastConsumer(objectMapper, fanoutPublisher, accountLookup);
    }

    @Test
    void payload에_userId_있으면_그대로_사용() {
        String envelope = "{\"payload\":{\"userId\":42,\"orderId\":1}}";
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_ORDER_ACCEPTED, 0, 0L, "k", envelope);

        consumer.onMessage(record, ack);

        ArgumentCaptor<StompFanoutMessage> captor = ArgumentCaptor.forClass(StompFanoutMessage.class);
        verify(fanoutPublisher).publish(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().destination()).isEqualTo("/queue/orders");
        assertThat(captor.getValue().payload()).isEqualTo(envelope);
        verifyNoInteractions(accountLookup);
        verify(ack).acknowledge();
    }

    @Test
    void userId_없고_accountId만_있으면_AccountLookup으로_조회() {
        String envelope = "{\"payload\":{\"accountId\":7,\"orderId\":1}}";
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_ORDER_FILLED, 0, 0L, "k", envelope);
        when(accountLookup.getUserId(7L)).thenReturn(99L);

        consumer.onMessage(record, ack);

        ArgumentCaptor<StompFanoutMessage> captor = ArgumentCaptor.forClass(StompFanoutMessage.class);
        verify(fanoutPublisher).publish(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(99L);
        verify(ack).acknowledge();
    }

    @Test
    void userId_도_accountId_도_없으면_skip하고_ack() {
        String envelope = "{\"payload\":{\"orderId\":1}}";
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_ORDER_REJECTED, 0, 0L, "k", envelope);

        consumer.onMessage(record, ack);

        verify(fanoutPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
        verify(ack).acknowledge();
    }

    @Test
    void AccountLookup이_예외_던져도_ack는_수행() {
        String envelope = "{\"payload\":{\"accountId\":7}}";
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>(KafkaConfig.TOPIC_ORDER_CANCELLED, 0, 0L, "k", envelope);
        when(accountLookup.getUserId(7L)).thenThrow(new RuntimeException("not found"));

        consumer.onMessage(record, ack);

        verify(fanoutPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
        verify(ack).acknowledge();
    }
}
