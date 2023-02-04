//package com.microservices.kafkaeventproducer.integration.controller;
//
//import com.microservices.kafkaeventproducer.domain.Item;
//import com.microservices.kafkaeventproducer.domain.ItemEvent;
//import com.microservices.kafkaeventproducer.domain.ItemEventType;
//import org.apache.kafka.clients.consumer.Consumer;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
//import org.springframework.kafka.test.EmbeddedKafkaBroker;
//import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.kafka.test.utils.KafkaTestUtils;
//import org.springframework.test.context.TestPropertySource;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
//@EmbeddedKafka(topics = {"item-event-topic"})
//@TestPropertySource(properties = {
//        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
//        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"
//})
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//class ItemEventsControllerIntegrationTest {
//
//    @Autowired
//    private TestRestTemplate restTemplate;
//
//    // IntelliJ gets confused finding this broker bean when @SpringBootTest is activated
//    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
//    @Autowired
//    private EmbeddedKafkaBroker embeddedKafkaBroker;
//
//    private Consumer<String, String> consumer;
//
//    @BeforeEach
//    void setUp() {
//        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));
//        consumer = new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), new StringDeserializer()).createConsumer();
//        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
//    }
//
//    @AfterEach
//    void tearDown() {
//        consumer.close();
//    }
//
//    @Test
//    void postItemEvent() {
//        ItemEvent itemEvent = ItemEvent.builder()
//                .eventId(null)
//                .item(Item.builder()
//                        .itemId(UUID.randomUUID())
//                        .itemName("Harry Potter")
//                        .itemOriginator("JK Rowling")
//                        .build())
//                .itemEventType(ItemEventType.CREATE)
//                .build();
//
//        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<ItemEvent> requestEntity = new HttpEntity<>(itemEvent, httpHeaders);
//
//        ResponseEntity<ItemEvent> responseEntity = restTemplate.exchange("/v1/item-event", HttpMethod.POST, requestEntity, ItemEvent.class);
//
//        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
//
//        ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(consumer, "item-event-topic");
//        assertNotNull(consumerRecord.value());
//    }
//
//    @Test
//    void updateItemEvent() {
//        ItemEvent itemEvent = ItemEvent.builder()
//                .eventId(UUID.randomUUID())
//                .item(Item.builder()
//                        .itemId(UUID.randomUUID())
//                        .itemName("Harry Potter")
//                        .itemOriginator("JK Rowling")
//                        .build())
//                .itemEventType(ItemEventType.CREATE)
//                .build();
//
//        HttpHeaders httpHeaders = new HttpHeaders();
//        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<ItemEvent> requestEntity = new HttpEntity<>(itemEvent, httpHeaders);
//
//        ResponseEntity<ItemEvent> responseEntity = restTemplate.exchange("/v1/item-event", HttpMethod.PUT, requestEntity, ItemEvent.class);
//
//        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
//
//        ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(consumer, "item-event-topic");
//        assertNotNull(consumerRecord.value());
//    }
//
//
//}