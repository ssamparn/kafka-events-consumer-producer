package com.microservices.kafkaeventconsumer.config;

import com.microservices.kafkaeventconsumer.service.ItemEventsRecoveryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@EnableKafka
@Configuration
public class ItemEventsConsumerConfig {

    @Autowired
    private ItemEventsRecoveryService recoveryService;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(ConcurrentKafkaListenerContainerFactoryConfigurer kafkaConfigurer,
                                                                                       ConsumerFactory<Object, Object> kafkaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaContainerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        kafkaConfigurer.configure(kafkaContainerFactory, kafkaConsumerFactory);
        kafkaContainerFactory.setConcurrency(3);
        kafkaContainerFactory.setErrorHandler((exception, data) -> {
            log.info("Exception in ConsumerConfig: {} and record is: {}", exception.getMessage(), data);
        });
        kafkaContainerFactory.setRetryTemplate(createRetryTemplate());
        kafkaContainerFactory.setRecoveryCallback((context -> {
           if (context.getLastThrowable().getCause() instanceof RecoverableDataAccessException) {
               log.info("Inside the recoverable logic");
               ConsumerRecord<String, String> consumerRecord = (ConsumerRecord<String, String>) context.getAttribute("record");
               assert consumerRecord != null;
               recoveryService.handleRecovery(consumerRecord);
           } else {
               log.info("Inside the non recoverable logic");
               throw new RuntimeException(context.getLastThrowable().getMessage());
           }
           return null;
        }));

        return kafkaContainerFactory;
    }

    @Bean
    public RetryTemplate createRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(1000);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        retryTemplate.setRetryPolicy(createRetryPolicy());

        return retryTemplate;
    }

    @Bean
    public RetryPolicy createRetryPolicy() {
        Map<Class<? extends Throwable>, Boolean> exceptionMap = new HashMap<>();
        exceptionMap.put(IllegalArgumentException.class, false);
        exceptionMap.put(RecoverableDataAccessException.class, true);

        return new SimpleRetryPolicy(3, exceptionMap, true);
    }
}
