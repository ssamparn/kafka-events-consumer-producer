package com.microservices.kafkaeventproducer.unit.producer;

import com.microservices.kafkaeventproducer.producer.ItemEventsProducer;
import com.microservices.kafkaeventproducer.util.TestUtil;
import com.microservices.kafkaevents.dto.ItemEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemEventsProducerUnitTest {

    private ItemEvent itemEvent;
    private ProducerRecord<String, ItemEvent> producerRecord;
    private RecordMetadata recordMetadata;
    private SendResult<String, ItemEvent> sendResult;

    @Mock
    private KafkaTemplate<String, ItemEvent> kafkaTemplate;
    @InjectMocks
    private ItemEventsProducer itemProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(itemProducer, "topic", "item-event-topic");
        itemEvent = TestUtil.newItemEventRecordWithItemEventId();

        producerRecord = new ProducerRecord<>("item-event-topic", itemEvent.getEventId().toString(), itemEvent);
        recordMetadata = new RecordMetadata(new TopicPartition("item-event-topic", 1), 1, 1, 342, System.currentTimeMillis(), 1, 2);
        sendResult = new SendResult<>(producerRecord, recordMetadata);
    }

    @Test
    void sendItemEventAnotherApproachTest_OnSuccess() {
        CompletableFuture<SendResult<String, ItemEvent>> expectedCompletableFuture = CompletableFuture.supplyAsync(() -> sendResult);

        when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(CompletableFuture.supplyAsync(() -> expectedCompletableFuture));

        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = itemProducer.sendItemEvent(itemEvent);

        actualCompletableFuture.whenComplete((successResult, ex) -> {
            if (ex != null) {
                var exception = assertThrows(Exception.class, actualCompletableFuture::get);
                assertEquals("Exception Calling Kafka", exception.getMessage());
            } else {
                // This code block will be executed in case of success
                SendResult<String, ItemEvent> sendResult1;
                try {
                    sendResult1 = actualCompletableFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                assert sendResult1.getRecordMetadata().partition() == 1;
            }
        });
    }

    @Test
    void sendItemEventAnotherApproachTest_OnFailure() {
        CompletableFuture<SendResult<String, ItemEvent>> expectedCompletableFuture = CompletableFuture.supplyAsync(() -> sendResult)
                .thenApply((sendResult1) -> {
                    throw new RuntimeException("Exception Calling Kafka");
                });

        when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(CompletableFuture.supplyAsync(() -> expectedCompletableFuture));

        CompletableFuture<SendResult<String, ItemEvent>> actualCompletableFuture = itemProducer.sendItemEvent(itemEvent);

        actualCompletableFuture.whenComplete((successResult, ex) -> {
            if (ex != null) {
                // This code block will be executed in case of failure
                var exception = assertThrows(Exception.class, actualCompletableFuture::get);
                assertEquals("Exception Calling Kafka", exception.getMessage());
            } else {
                SendResult<String, ItemEvent> sendResult1;
                try {
                    sendResult1 = actualCompletableFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                assert sendResult1.getRecordMetadata().partition() == 1;
            }
        });
    }
}