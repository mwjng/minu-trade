package com.minupay.trade.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    public static final String TOPIC_ORDER_ACCEPTED = "order.accepted";
    public static final String TOPIC_ORDER_REJECTED = "order.rejected";
    public static final String TOPIC_ORDER_FILLED = "order.filled";
    public static final String TOPIC_ORDER_CANCELLED = "order.cancelled";
    public static final String TOPIC_TRADE_EXECUTED = "trade.executed";
    public static final String TOPIC_HOLDING_UPDATED = "holding.updated";
    public static final String TOPIC_STOCK_UPDATED = "stock.updated";
    public static final String TOPIC_QUOTE_UPDATED = "quote.updated";

    @Value("${spring.kafka.bootstrap-servers:localhost:9093}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean public NewTopic orderAcceptedTopic()  { return TopicBuilder.name(TOPIC_ORDER_ACCEPTED).partitions(5).replicas(1).build(); }
    @Bean public NewTopic orderRejectedTopic()  { return TopicBuilder.name(TOPIC_ORDER_REJECTED).partitions(5).replicas(1).build(); }
    @Bean public NewTopic orderFilledTopic()    { return TopicBuilder.name(TOPIC_ORDER_FILLED).partitions(5).replicas(1).build(); }
    @Bean public NewTopic orderCancelledTopic() { return TopicBuilder.name(TOPIC_ORDER_CANCELLED).partitions(5).replicas(1).build(); }
    @Bean public NewTopic tradeExecutedTopic()  { return TopicBuilder.name(TOPIC_TRADE_EXECUTED).partitions(5).replicas(1).build(); }
    @Bean public NewTopic holdingUpdatedTopic() { return TopicBuilder.name(TOPIC_HOLDING_UPDATED).partitions(5).replicas(1).build(); }
    @Bean public NewTopic stockUpdatedTopic()   { return TopicBuilder.name(TOPIC_STOCK_UPDATED).partitions(3).replicas(1).build(); }
    @Bean public NewTopic quoteUpdatedTopic()   { return TopicBuilder.name(TOPIC_QUOTE_UPDATED).partitions(10).replicas(1).build(); }
}
