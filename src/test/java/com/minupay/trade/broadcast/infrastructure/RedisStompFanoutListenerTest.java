package com.minupay.trade.broadcast.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.broadcast.application.StompFanoutMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisStompFanoutListenerTest {

    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock Message redisMessage;

    ObjectMapper objectMapper = new ObjectMapper();
    RedisStompFanoutListener listener;

    @BeforeEach
    void setUp() {
        listener = new RedisStompFanoutListener(objectMapper, messagingTemplate);
    }

    @Test
    void public_메시지는_convertAndSend로_브로드캐스트() throws Exception {
        StompFanoutMessage fanout = StompFanoutMessage.publicMessage("/topic/quotes/005930", "{\"price\":1000}");
        when(redisMessage.getBody()).thenReturn(objectMapper.writeValueAsBytes(fanout));

        listener.onMessage(redisMessage, null);

        verify(messagingTemplate).convertAndSend("/topic/quotes/005930", "{\"price\":1000}");
        verify(messagingTemplate, never()).convertAndSendToUser(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void user_메시지는_convertAndSendToUser로_라우팅() throws Exception {
        StompFanoutMessage fanout = StompFanoutMessage.userMessage(42L, "/queue/orders", "{\"id\":1}");
        when(redisMessage.getBody()).thenReturn(objectMapper.writeValueAsBytes(fanout));

        listener.onMessage(redisMessage, null);

        verify(messagingTemplate).convertAndSendToUser("42", "/queue/orders", "{\"id\":1}");
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(),
                (Object) org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 잘못된_JSON_은_삼키고_라우팅하지_않는다() {
        when(redisMessage.getBody()).thenReturn("not-json".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(redisMessage, null);

        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(),
                (Object) org.mockito.ArgumentMatchers.any());
        verify(messagingTemplate, never()).convertAndSendToUser(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }
}
