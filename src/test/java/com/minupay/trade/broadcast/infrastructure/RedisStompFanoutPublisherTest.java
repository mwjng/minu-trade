package com.minupay.trade.broadcast.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.broadcast.application.StompFanoutMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisStompFanoutPublisherTest {

    @Mock StringRedisTemplate redisTemplate;
    ObjectMapper objectMapper = new ObjectMapper();
    RedisStompFanoutPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RedisStompFanoutPublisher(redisTemplate, objectMapper);
    }

    @Test
    void public_메시지를_직렬화하여_채널로_발행() {
        StompFanoutMessage message = StompFanoutMessage.publicMessage("/topic/quotes/005930", "{\"price\":1000}");

        publisher.publish(message);

        ArgumentCaptor<String> channel = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(channel.capture(), body.capture());
        assertThat(channel.getValue()).isEqualTo(RedisStompFanoutPublisher.CHANNEL);
        assertThat(body.getValue()).contains("\"destination\":\"/topic/quotes/005930\"");
        assertThat(body.getValue()).contains("\"userId\":null");
    }

    @Test
    void user_메시지에는_userId가_포함된다() {
        StompFanoutMessage message = StompFanoutMessage.userMessage(42L, "/queue/orders", "{\"id\":1}");

        publisher.publish(message);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq(RedisStompFanoutPublisher.CHANNEL), body.capture());
        assertThat(body.getValue()).contains("\"userId\":42");
        assertThat(body.getValue()).contains("\"destination\":\"/queue/orders\"");
    }
}
