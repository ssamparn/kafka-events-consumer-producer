//package com.microservices.kafkaeventconsumer.consumer;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.microservices.kafkaeventconsumer.entity.ItemEntity;
//import com.microservices.kafkaeventconsumer.entity.ItemEventEntity;
//import com.microservices.kafkaeventconsumer.entity.ItemEventTypeEntity;
//import com.microservices.kafkaeventconsumer.repository.ItemEventsRepository;
//import com.microservices.kafkaeventconsumer.consumer.ItemEventsConsumer;
//import com.microservices.kafkaeventconsumer.service.ItemEventsService;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.SpyBean;
//import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.test.EmbeddedKafkaBroker;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.kafka.test.utils.ContainerTestUtils;
//import org.springframework.test.context.TestPropertySource;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//
//import static org.mockito.ArgumentMatchers.isA;
//
//@Slf4j
//@EmbeddedKafka(topics = {"item-topic"})
//@TestPropertySource(properties = {
//        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
//        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}"
//})
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//class ItemEventsConsumerTest {
//
//    @SpyBean
//    private ItemEventsConsumer itemEventsConsumerSpy;
//
//    @SpyBean
//    private ItemEventsService itemEventsServiceSpy;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private KafkaTemplate<String, String> kafkaTemplate;
//
//    // IntelliJ gets confused finding this broker bean when @SpringBootTest is activated
//    @Autowired
//    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
//    private EmbeddedKafkaBroker embeddedKafkaBroker;
//
//    @Autowired
//    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
//    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
//
//    @Autowired
//    private ItemEventsRepository itemEventsRepository;
//
//    @BeforeEach
//    void setUp() {
//        kafkaListenerEndpointRegistry
//                .getListenerContainers()
//                .forEach(messageListenerContainer ->
//                        ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic()));
//    }
//
//    @AfterEach
//    void tearDown() {
//        itemEventsRepository.deleteAll();
//    }
//
//    @Test
//    void publishNewItemEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
//        String requestValue = "{\"eventId\":null,\"itemEventType\":\"CREATE\",\"item\":{\"itemId\":\"2866c984-b36d-42c3-9c51-5b8b11769c48\",\"itemName\":\"Harry Potter\",\"itemOriginator\":\"JK Rowling\"}}";
//        kafkaTemplate.sendDefault(requestValue).get();
//
//        CountDownLatch latch = new CountDownLatch(1);
//        latch.await(2, TimeUnit.SECONDS);
//
//        Mockito.verify(itemEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
//        Mockito.verify(itemEventsServiceSpy, Mockito.times(1)).processItemEvent(isA(ConsumerRecord.class));
//
//        List<ItemEventEntity> itemEventEntities = (List<ItemEventEntity>) itemEventsRepository.findAll();
//        Assertions.assertEquals(1, itemEventEntities.size());
//        itemEventEntities.forEach(itemEventEntity -> Assertions.assertNotNull(itemEventEntity.getItem()));
//    }
//
//    @Test
//    void updateItemEvent() throws ExecutionException, InterruptedException, JsonProcessingException {
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
//        CountDownLatch latch = new CountDownLatch(1);
//        latch.await(2, TimeUnit.SECONDS);
//
//        Mockito.verify(itemEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
//        Mockito.verify(itemEventsServiceSpy, Mockito.times(1)).processItemEvent(isA(ConsumerRecord.class));
//
//        ItemEventEntity entity = itemEventsRepository.findById(inputItemEvent.getEventId()).get();
//        Assertions.assertNotNull(entity);
//    }
//
//    @Test
//    void publishInvalidItemEvent() throws JsonProcessingException, InterruptedException, ExecutionException {
//        String nonExistantEventId = "2866c984-b36d-42c3-9c51-5b8b11769c48";
//        String requestValue = "{\"eventId\":" + nonExistantEventId + ",\"itemEventType\":\"UPDATE\",\"item\":{\"itemId\":\"2866c984-b36d-42c3-9c51-5b8b11769c48\",\"itemName\":\"Harry Potter\",\"itemOriginator\":\"JK Rowling\"}}";
//        kafkaTemplate.sendDefault(nonExistantEventId, requestValue).get();
//
//        CountDownLatch latch = new CountDownLatch(1);
//        latch.await(2, TimeUnit.SECONDS);
//
//        Mockito.verify(itemEventsConsumerSpy, Mockito.times(1)).onMessage(isA(ConsumerRecord.class));
//        Mockito.verify(itemEventsServiceSpy, Mockito.times(1)).processItemEvent(isA(ConsumerRecord.class));
//
//        Optional<ItemEventEntity> entity = itemEventsRepository.findById(UUID.fromString(nonExistantEventId));
//        Assertions.assertFalse(entity.isPresent());
//    }
//
//    @Test
//    void publishNullItemEvent() throws JsonProcessingException, InterruptedException, ExecutionException {
//        String nonExistantEventId = null;
//        String requestValue = "{\"eventId\":" + nonExistantEventId + ",\"itemEventType\":\"UPDATE\",\"item\":{\"itemId\":\"2866c984-b36d-42c3-9c51-5b8b11769c48\",\"itemName\":\"Harry Potter\",\"itemOriginator\":\"JK Rowling\"}}";
//        kafkaTemplate.sendDefault(nonExistantEventId, requestValue).get();
//
//        CountDownLatch latch = new CountDownLatch(1);
//        latch.await(2, TimeUnit.SECONDS);
//
//        Mockito.verify(itemEventsConsumerSpy, Mockito.times(2)).onMessage(isA(ConsumerRecord.class));
//        Mockito.verify(itemEventsServiceSpy, Mockito.times(2)).processItemEvent(isA(ConsumerRecord.class));
//    }
//}