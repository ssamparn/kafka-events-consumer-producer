package com.microservices.kafkaeventproducer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Profile("local")
@Configuration
public class AutoCreateConfig {

    @Bean
    public NewTopic itemEventTopic() {
        return TopicBuilder.name("item-event-topic")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
