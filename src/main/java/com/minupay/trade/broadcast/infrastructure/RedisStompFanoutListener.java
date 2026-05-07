package com.minupay.trade.broadcast.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.broadcast.application.StompFanoutMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStompFanoutListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            StompFanoutMessage fanout = objectMapper.readValue(
                    new String(message.getBody(), StandardCharsets.UTF_8), StompFanoutMessage.class);
            dispatch(fanout);
        } catch (Exception e) {
            log.warn("STOMP fanout 처리 실패", e);
        }
    }

    private void dispatch(StompFanoutMessage fanout) {
        if (fanout.isUserDestination()) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(fanout.userId()), fanout.destination(), fanout.payload());
            return;
        }
        messagingTemplate.convertAndSend(fanout.destination(), fanout.payload());
    }
}
