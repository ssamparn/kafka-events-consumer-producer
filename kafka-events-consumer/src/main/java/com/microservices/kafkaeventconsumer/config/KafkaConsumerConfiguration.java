package com.microservices.kafkaeventconsumer.config;

import com.microservices.kafkaeventconsumer.service.FailureService;
import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@Profile("local")
public class KafkaConsumerConfiguration {

    public static final String RETRY = "RETRY";
    public static final String SUCCESS = "SUCCESS";
    public static final String DEAD = "DEAD";

    @Value("${spring.kafka.consumer.bootstrap-servers: localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id: item-events-listener-group}")
    private String groupId;

    @Value("${topics.retry:item-event-topic.retry}")
    private String retryTopic;

    @Value("${topics.dlt:item-event-topic.dlt}")
    private String deadLetterTopic;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private FailureService failureService;

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
        kafkaContainerFactory.setCommonErrorHandler(errorHandler());
        return kafkaContainerFactory;
    }

    public DefaultErrorHandler errorHandler() {
        // Exponential BackOff settings
        ExponentialBackOffWithMaxRetries exponentialBackOff = new ExponentialBackOffWithMaxRetries(2);
        exponentialBackOff.setInitialInterval(1_000L);
        exponentialBackOff.setMultiplier(2.0);
        exponentialBackOff.setMaxInterval(2_000L);

        // Fixed BackOff settings
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 2L); // Retry twice in an interval of 1 second

        // Error Handler with the Fixed BackOff
        // Provide either fixed backoff config or exponential backoff config
        DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(
//                publishingRecoverer(), // to be used when pushing events to retry or dead letter topic
                consumerRecordRecoverer,
                fixedBackOff
//                exponentialBackOff
        );

        // provide list of exceptions, from which the kafka consumer is not going to recover from.
        List<Class<IllegalArgumentException>> exceptionListToIgnore = List.of(IllegalArgumentException.class);
        // Error Handler for not to retry for certain exceptions
        exceptionListToIgnore.forEach(defaultErrorHandler::addNotRetryableExceptions);

        // setting retry listeners.
        defaultErrorHandler.setRetryListeners(
                (record, ex, deliveryAttempt) -> log.info("Failed record in Retry Listener exception : {} , deliveryAttempt : {} ", ex.getMessage(), deliveryAttempt)
        );

        return defaultErrorHandler;
    }

    public DeadLetterPublishingRecoverer publishingRecoverer() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, (r, e) -> {
            log.error("Exception in publishingRecoverer : {} ", e.getMessage(), e);
            if (e.getCause() instanceof RecoverableDataAccessException) { // thrown if the event id is "b9c21087-3391-46d4-91b7-5b493c057089"
                return new TopicPartition(retryTopic, r.partition());
            } else {
                return new TopicPartition(deadLetterTopic, r.partition());
            }
        });
        return recoverer;
    }

    public ConsumerRecordRecoverer consumerRecordRecoverer = (record, exception) -> {
        log.error("failed Record : {} failing with exception : {}", record, exception.getMessage());
        if (exception.getCause() instanceof RecoverableDataAccessException) {
            // Add recovery logic here. e.g: persist the failed record in database
            log.info("Inside the recoverable logic");
            failureService.saveFailureRecord((ConsumerRecord<String, ItemEvent>) record, exception, RETRY);

        } else {
            log.info("Inside the non recoverable logic and skipping the record : {}", record);
            failureService.saveFailureRecord((ConsumerRecord<String, ItemEvent>) record, exception, DEAD);
        }
    };

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
