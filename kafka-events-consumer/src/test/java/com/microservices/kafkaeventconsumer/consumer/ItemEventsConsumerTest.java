package com.microservices.kafkaeventconsumer.consumer;

import com.microservices.kafkaeventconsumer.entity.ItemEntity;
import com.microservices.kafkaeventconsumer.entity.ItemEventEntity;
import com.microservices.kafkaeventconsumer.entity.ItemEventTypeEntity;
import com.microservices.kafkaeventconsumer.repository.ItemEventsRepository;
import com.microservices.kafkaeventconsumer.service.ItemEventsService;
import com.microservices.kafkaevents.dto.ItemEvent;
import com.microservices.kafkaevents.dto.ItemEventType;
import com.microservices.kafkaevents.util.ItemEventsUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
        "spring.kafka.template.default-topic=${spring.kafka.template.default-topic}",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JacksonJsonDeserializer"
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ItemEventsConsumerTest {

    private Consumer<String, String> consumer;

    @Value("${spring.topics.default}")
    private String defaultTopic;

    @Value("${spring.topics.retry}")
    private String retryTopic;

    @Value("${spring.topics.dlt}")
    private String deadLetterTopic;

    @MockitoSpyBean
    private ItemEventsConsumer itemEventsConsumerSpy;

    @MockitoSpyBean
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
        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = kafkaTemplate.send(defaultTopic, ItemEventsUtil.createItemEvent());

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

    @Test
    void updateItemEvent() throws InterruptedException, ExecutionException {
        // Arrange
        ItemEvent itemEventDto = ItemEventsUtil.createItemEventWithEventId();
        UUID eventId = itemEventDto.eventId();
        UUID itemId = itemEventDto.item().itemId();

        // Save initial state in DB
        ItemEventEntity itemEventEntity = ItemEventEntity.builder()
                .eventId(eventId)
                .itemEventType(ItemEventTypeEntity.CREATE)
                .item(ItemEntity.builder()
                        .itemId(itemId)
                        .itemName(itemEventDto.item().itemName())
                        .itemOriginator(itemEventDto.item().itemOriginator())
                        .build())
                .build();
        itemEventEntity.getItem().setItemEvent(itemEventEntity);
        ItemEventEntity savedEntity = itemEventsRepository.save(itemEventEntity);
        Integer initialVersion = savedEntity.getVersion();

        // Prepare the update event DTO with different values
        String updatedName = "Harry Potter and the Goblet of Fire";
        String updatedOriginator = "Joanne Rowling";
        com.microservices.kafkaevents.dto.Item updatedItemDto = new com.microservices.kafkaevents.dto.Item(itemId, updatedName, updatedOriginator);
        ItemEvent updatedItemEventDto = new ItemEvent(eventId, updatedItemDto, ItemEventType.UPDATE);

        // Act
        kafkaTemplate.send(defaultTopic, String.valueOf(eventId), updatedItemEventDto).get();

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(3, TimeUnit.SECONDS);

        // Assert
        Mockito.verify(itemEventsConsumerSpy, Mockito.atLeastOnce()).onMessage(isA(ConsumerRecord.class));
        Mockito.verify(itemEventsServiceSpy, Mockito.atLeastOnce()).processItemEvent(isA(ConsumerRecord.class));

        ItemEventEntity entity = itemEventsRepository.findById(eventId).orElseThrow();
        
        Assertions.assertAll("Verify updated ItemEventEntity",
                () -> Assertions.assertEquals(eventId, entity.getEventId()),
                () -> Assertions.assertEquals(ItemEventTypeEntity.UPDATE, entity.getItemEventType()),
                () -> Assertions.assertNotNull(entity.getItem()),
                () -> Assertions.assertEquals(itemId, entity.getItem().getItemId()),
                () -> Assertions.assertEquals(updatedName, entity.getItem().getItemName()),
                () -> Assertions.assertEquals(updatedOriginator, entity.getItem().getItemOriginator()),
                () -> Assertions.assertTrue(entity.getVersion() > initialVersion, "Version should be incremented")
        );
    }

    @Test
    void publishInvalidItemEvent() throws InterruptedException {
        String nonExistantEventId = "2866c984-b36d-42c3-9c51-5b8b11769c48";
        // Arrange
        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = kafkaTemplate.send(defaultTopic, nonExistantEventId, ItemEventsUtil.createItemEventWithInvalidItem());

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
        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = kafkaTemplate.send(defaultTopic, ItemEventsUtil.updateItemEventWithNullEventId());

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
    void publishItemEventToRetryTopicTest() throws InterruptedException {
        // Arrange
        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = kafkaTemplate.send(defaultTopic, ItemEventsUtil.updateItemEventWithEventId(UUID.fromString("b9c21087-3391-46d4-91b7-5b493c057089")));

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

                Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps(embeddedKafkaBroker, "group1", true));
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
        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = kafkaTemplate.send(defaultTopic, ItemEventsUtil.updateItemEventWithNullEventId());

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

                Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps(embeddedKafkaBroker, "group2", true));
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
}