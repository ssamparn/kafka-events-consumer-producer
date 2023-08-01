package com.microservices.kafkaeventconsumer.consumer;

import com.microservices.kafkaeventconsumer.entity.ItemEventEntity;
import com.microservices.kafkaeventconsumer.repository.FailureRecordRepository;
import com.microservices.kafkaeventconsumer.repository.ItemEventsRepository;
import com.microservices.kafkaeventconsumer.service.ItemEventsService;
import com.microservices.kafkaevents.dto.ItemEvent;
import com.microservices.kafkaevents.util.ItemEventsUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;

@Slf4j
@EmbeddedKafka(topics = {"item-event-topic", "item-event-topic.retry", "item-event-topic.dlt"}, partitions = 3)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer"
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ItemEventsConsumerTest {

    private Consumer<String, String> consumer;

    @Value("${topics.retry}")
    private String retryTopic;

    @Value("${topics.dlt}")
    private String deadLetterTopic;

    @SpyBean
    private ItemEventsConsumer itemEventsConsumerSpy;

    @SpyBean
    private ItemEventsService itemEventsServiceSpy;

    @Autowired
    private KafkaTemplate<String, ItemEvent> kafkaTemplate;

    // IntelliJ gets confused finding this broker bean when @SpringBootTest is activated
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private ItemEventsRepository itemEventsRepository;

    @Autowired
    private FailureRecordRepository failureRecordRepository;

    @BeforeEach
    void setUp() {
        kafkaListenerEndpointRegistry.getListenerContainers()
                .forEach(messageListenerContainer ->
                        ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    @AfterEach
    void tearDown() {
        itemEventsRepository.deleteAll();
        failureRecordRepository.deleteAll();
    }

    @Test
    void publishNewItemEvent() throws InterruptedException {
        // Arrange
        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = kafkaTemplate.sendDefault(ItemEventsUtil.itemEventRecord());

        // Act
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(2, TimeUnit.SECONDS); // blocking the thread for 2 seconds

        actualCompletableFuture.whenComplete((successResult, ex) -> {
            if (ex != null) {
                var exception = assertThrows(Exception.class, actualCompletableFuture::get);
                assertEquals("Exception Calling Kafka", exception.getMessage());
            } else {
                // This code block will be executed in case of success
                // Assert
                verify(itemEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
                verify(itemEventsServiceSpy, Mockito.times(1)).processItemEvent(isA(ConsumerRecord.class));

                List<ItemEventEntity> itemEventEntities = itemEventsRepository.findAll();
                Assertions.assertEquals(1, itemEventEntities.size());
                itemEventEntities.forEach(itemEventEntity -> Assertions.assertNotNull(itemEventEntity.getItem()));
            }
        });
    }

//    @Test
//    void updateItemEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
//        // Arrange
//        String itemEventStr = "{\"eventId\":null,\"itemEventType\":\"CREATE\",\"item\":{\"itemId\":\"2866c984-b36d-42c3-9c51-5b8b11769c48\",\"itemName\":\"Harry Potter\",\"itemOriginator\":\"JK Rowling\"}}";
//        ItemEventEntity inputItemEvent = objectMapper.readValue(itemEventStr, ItemEventEntity.class);
//        inputItemEvent.getItem().setItemEvent(inputItemEvent);
//
//        itemEventsRepository.save(inputItemEvent);
//
//        ItemEntity toBeUpdatedItem = ItemEntity.builder().itemId(UUID.fromString("2866c984-b36d-42c3-9c51-5b8b11769c48")).itemName("Harry Potter").itemOriginator("JK Rowling").build();
//
//        inputItemEvent.setItem(toBeUpdatedItem);
//        inputItemEvent.setItemEventType(ItemEventTypeEntity.UPDATE);
//
//
//        String toBeUpdatedInputItemEvent = objectMapper.writeValueAsString(inputItemEvent);
//
//        kafkaTemplate.sendDefault(String.valueOf(inputItemEvent.getEventId()), toBeUpdatedInputItemEvent).get();
//
//        // Act
//        CountDownLatch latch = new CountDownLatch(1);
//        latch.await(2, TimeUnit.SECONDS);
//
//        // Assert
//        Mockito.verify(itemEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
//        Mockito.verify(itemEventsServiceSpy, Mockito.times(1)).processItemEvent(isA(ConsumerRecord.class));
//
//        ItemEventEntity entity = itemEventsRepository.findById(inputItemEvent.getEventId()).get();
//        Assertions.assertNotNull(entity);
//    }

    @Test
    void publishInvalidItemEvent() throws InterruptedException {
        String nonExistantEventId = "2866c984-b36d-42c3-9c51-5b8b11769c48";
        // Arrange
        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = kafkaTemplate.sendDefault(nonExistantEventId, ItemEventsUtil.itemEventRecordWithInvalidItem());

        // Act
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);

        // Assert
        actualCompletableFuture.whenComplete((successResult, ex) -> {
            if (ex != null) {
                var exception = assertThrows(Exception.class, actualCompletableFuture::get);
                assertEquals("Exception Calling Kafka", exception.getMessage());
            } else {
                // This code block will be executed in case of success
                // Assert
                verify(itemEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
                verify(itemEventsServiceSpy, Mockito.times(1)).processItemEvent(isA(ConsumerRecord.class));

                Optional<ItemEventEntity> entity = itemEventsRepository.findById(UUID.fromString(nonExistantEventId));
                Assertions.assertFalse(entity.isPresent());
            }
        });
    }

    @Test
    void publishNullItemEvent() throws InterruptedException {
        // Arrange
        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = kafkaTemplate.sendDefault(ItemEventsUtil.itemEventRecordUpdateWithNullItemEventId());

        // Act
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);

        // Assert
        actualCompletableFuture.whenComplete((successResult, ex) -> {
            if (ex != null) {
                var exception = assertThrows(Exception.class, actualCompletableFuture::get);
                assertEquals("Exception Calling Kafka", exception.getMessage());
            } else {
                // Assert
                verify(itemEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
                verify(itemEventsServiceSpy, Mockito.times(1)).processItemEvent(isA(ConsumerRecord.class));
            }
        });
    }

    @Test
    @Disabled // To be enabled when events are to be published to retry topic. In this branch, the failed messages are to be pushed to database.
    void publishItemEventToRetryTopicTest() throws InterruptedException {
        // Arrange
        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = kafkaTemplate.sendDefault(ItemEventsUtil.itemEventRecordUpdateWithProvidedEventId(UUID.fromString("b9c21087-3391-46d4-91b7-5b493c057089")));

        // Act
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(10, TimeUnit.SECONDS);

        // Assert
        actualCompletableFuture.whenComplete((successResult, ex) -> {
            if (ex != null) {
                var exception = assertThrows(Exception.class, actualCompletableFuture::get);
                assertEquals("Exception Calling Kafka", exception.getMessage());
            } else {
                verify(itemEventsConsumerSpy, Mockito.times(3)).onMessage(isA(ConsumerRecord.class));
                verify(itemEventsServiceSpy, Mockito.times(3)).processItemEvent(isA(ConsumerRecord.class));

                Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));
                consumer = new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), new StringDeserializer()).createConsumer();
                embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, retryTopic);

                ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(consumer, retryTopic);

                log.info("consumer record in retry topic: {}", consumerRecord.value());
                consumerRecord.headers()
                    .forEach(header -> {
                        System.out.println("Header Key : " + header.key() + ", Header Value : " + new String(header.value()));
                    });
            }
        });
    }

    @Test
    void publishItemEventToDeadLetterTopicTest() throws InterruptedException {
        // Arrange
        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = kafkaTemplate.sendDefault(ItemEventsUtil.itemEventRecordUpdateWithNullItemEventId());

        // Act
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(10, TimeUnit.SECONDS);

        // Assert
        actualCompletableFuture.whenComplete((successResult, ex) -> {
            if (ex != null) {
                var exception = assertThrows(Exception.class, actualCompletableFuture::get);
                assertEquals("Exception Calling Kafka", exception.getMessage());
            } else {
                verify(itemEventsConsumerSpy, Mockito.times(3)).onMessage(isA(ConsumerRecord.class));
                verify(itemEventsServiceSpy, Mockito.times(3)).processItemEvent(isA(ConsumerRecord.class));

                Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group2", "true", embeddedKafkaBroker));
                consumer = new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), new StringDeserializer()).createConsumer();
                embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, deadLetterTopic);

                ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(consumer, deadLetterTopic);

                log.info("consumer record in dead letter topic: {}", consumerRecord.value());
                consumerRecord.headers()
                        .forEach(header -> {
                            System.out.println("Header Key : " + header.key() + ", Header Value : " + new String(header.value()));
                        });
            }
        });
    }

    @Test
    void persistFailedItemEventInDatabaseTest() throws InterruptedException {
        // Arrange
        kafkaTemplate.sendDefault(ItemEventsUtil.itemEventRecordUpdateWithNullItemEventId());

        // Act
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(10, TimeUnit.SECONDS);

        // Assert
        verify(itemEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
        verify(itemEventsServiceSpy, Mockito.times(1)).processItemEvent(isA(ConsumerRecord.class));

        long failureCount = failureRecordRepository.count();
        assertEquals(1, failureCount);
        failureRecordRepository.findAll().forEach(failureRecord -> log.info("failure record persisted: {}", failureRecord));
    }

}