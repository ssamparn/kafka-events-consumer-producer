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
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EmbeddedKafka(topics = {"item-event-topic"})
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ItemEventsControllerIntegrationTest {

    private ObjectMapper objectMapper;

    @Autowired
    private WebTestClient webTestClient;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps(embeddedKafkaBroker, "group1", true));
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @Test
    void postItemEvent() {
        ItemEvent itemEvent = TestUtil.itemEventRecord();

        webTestClient.post()
                .uri("/api/v1/item-event")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(itemEvent)
                .exchange()
                .expectStatus().isCreated();

        ConsumerRecords<String, String> consumerRecords = KafkaTestUtils.getRecords(consumer);

        assertNotNull(consumerRecords);
        consumerRecords.forEach(record -> {
            var itemEventActual = TestUtil.parseItemEventRecord(objectMapper, record.value());
            assertNotNull(itemEventActual.eventId());
            assertEquals(itemEvent.item().itemName(), itemEventActual.item().itemName());
            assertEquals(itemEvent.item().itemOriginator(), itemEventActual.item().itemOriginator());
        });
    }

    @Test
    void updateItemEvent() {
        ItemEvent itemEvent = TestUtil.itemEventRecordUpdate();

        webTestClient.put()
                .uri("/api/v1/item-event")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(itemEvent)
                .exchange()
                .expectStatus().isOk();

        ConsumerRecords<String, String> consumerRecords = KafkaTestUtils.getRecords(consumer);

        assertNotNull(consumerRecords);
        consumerRecords.forEach(record -> {
            var itemEventActual = TestUtil.parseItemEventRecord(objectMapper, record.value());
            assertNotNull(itemEventActual.eventId());
            assertEquals(itemEvent.item().itemName(), itemEventActual.item().itemName());
            assertEquals(itemEvent.item().itemOriginator(), itemEventActual.item().itemOriginator());
        });
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }
}
