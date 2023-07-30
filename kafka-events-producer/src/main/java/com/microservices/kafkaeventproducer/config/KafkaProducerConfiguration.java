package com.microservices.kafkaeventproducer.config;

import com.microservices.kafkaevents.dto.ItemEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Profile("local")
@Configuration
public class KafkaProducerConfiguration {

    @Value("${spring.kafka.producer.bootstrap-servers: localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.template.default-topic}")
    private String topic;

    @Bean
    public NewTopic itemEventTopic() {
        return TopicBuilder.name(topic)
                .partitions(1)
                .replicas(3)
                .build();
    }

    @Bean
    public ProducerFactory<String, ItemEvent> itemEventProducerFactory() {
        Map<String, Object> producerConfigProps = new HashMap<>();
        producerConfigProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfigProps.put(ProducerConfig.ACKS_CONFIG, "all"); // acks: all is same as acks = -1
        producerConfigProps.put(ProducerConfig.RETRIES_CONFIG, 10);
        producerConfigProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfigProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(producerConfigProps);
    }

    @Bean
    public KafkaTemplate<String, ItemEvent> kafkaTemplate() {
        return new KafkaTemplate<>(itemEventProducerFactory());
    }

}
