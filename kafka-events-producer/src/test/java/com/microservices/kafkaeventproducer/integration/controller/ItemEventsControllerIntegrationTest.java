package com.microservices.kafkaeventproducer.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.kafkaeventproducer.util.TestUtil;
import com.microservices.kafkaevents.dto.ItemEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EmbeddedKafka(topics = {"item-event-topic"})
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ItemEventsControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate restTemplate;

    // IntelliJ gets confused finding this broker bean when @SpringBootTest is activated
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void postItemEvent() {
        ItemEvent itemEvent = TestUtil.itemEventRecord();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ItemEvent> requestEntity = new HttpEntity<>(itemEvent, httpHeaders);

        ResponseEntity<ItemEvent> responseEntity = restTemplate.exchange("/api/v1/item-event", HttpMethod.POST, requestEntity, ItemEvent.class);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        // single consumer record
        // ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(consumer, "item-event-topic");
        ConsumerRecords<String, String> consumerRecords = KafkaTestUtils.getRecords(consumer);

        assertNotNull(consumerRecords);

        consumerRecords.forEach(record -> {
            var itemEventActual = TestUtil.parseItemEventRecord(objectMapper, record.value());
            assertNotNull(itemEventActual.getEventId());
            assertEquals(itemEvent.getItem().getItemName(), itemEventActual.getItem().getItemName());
            assertEquals(itemEvent.getItem().getItemOriginator(), itemEventActual.getItem().getItemOriginator());
        });
    }

    @Test
    void updateItemEvent() {
        ItemEvent itemEvent = TestUtil.itemEventRecordUpdate();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ItemEvent> requestEntity = new HttpEntity<>(itemEvent, httpHeaders);

        ResponseEntity<ItemEvent> responseEntity = restTemplate.exchange("/api/v1/item-event", HttpMethod.PUT, requestEntity, ItemEvent.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // single consumer record
        // ConsumerRecord<String, String> consumerRecord = KafkaTestUtils.getSingleRecord(consumer, "item-event-topic");
        ConsumerRecords<String, String> consumerRecords = KafkaTestUtils.getRecords(consumer);

        assertNotNull(consumerRecords);

        consumerRecords.forEach(record -> {
            var itemEventActual = TestUtil.parseItemEventRecord(objectMapper, record.value());
            assertNotNull(itemEventActual.getEventId());
            assertEquals(itemEvent.getItem().getItemName(), itemEventActual.getItem().getItemName());
            assertEquals(itemEvent.getItem().getItemOriginator(), itemEventActual.getItem().getItemOriginator());
        });
    }
}