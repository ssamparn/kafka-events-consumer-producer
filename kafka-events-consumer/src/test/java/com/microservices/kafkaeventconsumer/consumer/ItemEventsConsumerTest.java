package com.microservices.kafkaeventconsumer.consumer;

import com.microservices.kafkaeventconsumer.entity.ItemEventEntity;
import com.microservices.kafkaeventconsumer.repository.ItemEventsRepository;
import com.microservices.kafkaeventconsumer.service.ItemEventsService;
import com.microservices.kafkaevents.dto.ItemEvent;
import com.microservices.kafkaevents.util.ItemEventsUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;

@Slf4j
@EmbeddedKafka(topics = {"item-event-topic"}, partitions = 3)
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

    @BeforeEach
    void setUp() {
        kafkaListenerEndpointRegistry.getListenerContainers()
                .forEach(messageListenerContainer ->
                        ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    @AfterEach
    void tearDown() {
        itemEventsRepository.deleteAll();
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
                Mockito.verify(itemEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
                Mockito.verify(itemEventsServiceSpy, Mockito.times(1)).processItemEvent(isA(ConsumerRecord.class));

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
                Mockito.verify(itemEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
                Mockito.verify(itemEventsServiceSpy, Mockito.times(1)).processItemEvent(isA(ConsumerRecord.class));

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
                Mockito.verify(itemEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
                Mockito.verify(itemEventsServiceSpy, Mockito.times(1)).processItemEvent(isA(ConsumerRecord.class));
            }
        });
    }

    @Test
    void doNotRetryOnExceptionsTest() throws InterruptedException {
        // Arrange
        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = kafkaTemplate.sendDefault(ItemEventsUtil.itemEventRecordUpdateWithProvidedEventId(UUID.fromString("b9c21087-3391-46d4-91b7-5b493c057089")));

        // Act
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);

        // Assert
        actualCompletableFuture.whenComplete((successResult, ex) -> {
            if (ex != null) {
                var exception = assertThrows(Exception.class, actualCompletableFuture::get);
                assertEquals("Exception Calling Kafka", exception.getMessage());
            } else {
                Mockito.verify(itemEventsConsumerSpy, Mockito.times(3)).onMessage(isA(ConsumerRecord.class));
                Mockito.verify(itemEventsServiceSpy, Mockito.times(3)).processItemEvent(isA(ConsumerRecord.class));
            }
        });
    }
}