package com.microservices.kafkaeventconsumer.config;

import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@Profile("local")
@EnableKafka
public class KafkaConsumerConfiguration {

    @Value("${spring.kafka.consumer.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:item-events-listener-group}")
    private String groupId;

    @Value("${spring.topics.retry:item-event-topic.retry}")
    private String retryTopic;

    @Value("${spring.topics.dlt:item-event-topic.dlt}")
    private String deadLetterTopic;

    /**
     * Producer configuration only to be used for sending messages to retry topic, dead letter topic and for integration tests as well.
     * */
    @Bean
    public ProducerFactory<String, ItemEvent> itemEventProducerFactory() {
        Map<String, Object> producerConfigProps = new HashMap<>();
        producerConfigProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfigProps.put(ProducerConfig.ACKS_CONFIG, "all"); // acks: all is same as acks = -1
        producerConfigProps.put(ProducerConfig.RETRIES_CONFIG, 10);
        producerConfigProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfigProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(producerConfigProps);
    }

    @Bean
    public KafkaTemplate<String, ItemEvent> kafkaTemplate() {
        return new KafkaTemplate<>(itemEventProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, ItemEvent> consumerFactory() {
        Map<String, Object> consumerConfigProps = new HashMap<>();
        consumerConfigProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerConfigProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerConfigProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerConfigProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerConfigProps.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerConfigProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerConfigProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(consumerConfigProps, new StringDeserializer(), new JacksonJsonDeserializer<>(ItemEvent.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ItemEvent> kafkaListenerContainerFactory(KafkaTemplate <String, ItemEvent> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, ItemEvent> kafkaContainerFactory = new ConcurrentKafkaListenerContainerFactory<>();
        kafkaContainerFactory.setConsumerFactory(consumerFactory());
//        kafkaContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL); // here we are overriding the acknowledgement mode of kafka.
//        Default acknowledgement mode of kafka is BATCH
        kafkaContainerFactory.setConcurrency(3);
        kafkaContainerFactory.setCommonErrorHandler(errorHandler(kafkaTemplate));
        return kafkaContainerFactory;
    }

    public DefaultErrorHandler errorHandler(KafkaTemplate<String, ItemEvent> kafkaTemplate) {
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
                publishingRecoverer(kafkaTemplate),
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

    public DeadLetterPublishingRecoverer publishingRecoverer(KafkaTemplate<String, ItemEvent> kafkaTemplate) {
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
