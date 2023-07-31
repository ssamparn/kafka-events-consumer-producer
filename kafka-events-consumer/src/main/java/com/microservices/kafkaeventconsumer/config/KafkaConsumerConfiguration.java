package com.microservices.kafkaeventconsumer.config;

import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@Profile("local")
public class KafkaConsumerConfiguration {

    @Value("${spring.kafka.consumer.bootstrap-servers: localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id: item-events-listener-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, ItemEvent> consumerFactory() {
        Map<String, Object> consumerConfigProps = new HashMap<>();
        consumerConfigProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfigProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerConfigProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerConfigProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerConfigProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerConfigProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerConfigProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(consumerConfigProps, new StringDeserializer(), new JsonDeserializer<>(ItemEvent.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ItemEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ItemEvent> kafkaContainerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        kafkaContainerFactory.setConsumerFactory(consumerFactory());
//        kafkaContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL); // here we are overriding the acknowledgement mode of kafka.
//        Default acknowledgement mode of kafka is BATCH
        kafkaContainerFactory.setConcurrency(3);
        return kafkaContainerFactory;
    }

//    @Bean
//    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
//    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
//                                                                                       ConsumerFactory<Object, Object> kafkaConsumerFactory) {
//        ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaContainerFactory = new ConcurrentKafkaListenerContainerFactory<>();
//        configurer.configure(kafkaContainerFactory, kafkaConsumerFactory);
//        kafkaContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL); // Here we are manually managing the offset. Just for demo purpose
//        kafkaContainerFactory.setConcurrency(3);
//
//        return kafkaContainerFactory;
//    }

//    @Bean
//    public RetryTemplate createRetryTemplate() {
//        RetryTemplate retryTemplate = new RetryTemplate();
//
//        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
//        fixedBackOffPolicy.setBackOffPeriod(1000);
//        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
//        retryTemplate.setRetryPolicy(createRetryPolicy());
//
//        return retryTemplate;
//    }
//
//    @Bean
//    public RetryPolicy createRetryPolicy() {
//        Map<Class<? extends Throwable>, Boolean> exceptionMap = new HashMap<>();
//        exceptionMap.put(IllegalArgumentException.class, false);
//        exceptionMap.put(RecoverableDataAccessException.class, true);
//
//        return new SimpleRetryPolicy(3, exceptionMap, true);
//    }
}
