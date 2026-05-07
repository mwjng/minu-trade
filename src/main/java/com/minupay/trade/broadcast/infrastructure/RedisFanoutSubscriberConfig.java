package com.minupay.trade.broadcast.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.ChannelTopic;

@Configuration
@RequiredArgsConstructor
public class RedisFanoutSubscriberConfig {

    private final RedisConnectionFactory connectionFactory;
    private final RedisStompFanoutListener listener;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        Topic topic = new ChannelTopic(RedisStompFanoutPublisher.CHANNEL);
        container.addMessageListener(listener, topic);
        return container;
    }
}
