package com.microservices.kafkaeventproducer.producer;

import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemEventsProducer {

    @Value("${spring.kafka.template.default-topic}")
    private String topic;

    private final KafkaTemplate<String, ItemEvent> kafkaTemplate;

    public CompletableFuture<SendResult<String, ItemEvent>> sendItemEvent(ItemEvent itemEvent) {
        ProducerRecord<String, ItemEvent> producerRecord = buildProducerRecord(itemEvent);

        // with kafkaTemplate.send(), there are actually 2 rest calls happens behind the scene.
        // 1. Blocking Call: To get the metadata of the kafka cluster.
        // 2. Async Call: To Send Message to kafka topic if the first call goes fine.

        CompletableFuture<SendResult<String, ItemEvent>> sendResultCompletableFuture = kafkaTemplate.send(producerRecord);

        return sendResultCompletableFuture.whenComplete((successResult, exception) -> {
            if (exception == null) {
                handleSuccess(successResult, itemEvent);
            } else {
                handleFailure(exception, itemEvent);
            }
        });
    }

    private ProducerRecord<String, ItemEvent> buildProducerRecord(ItemEvent itemEvent) {
        List<Header> requestHeader = List.of(new RecordHeader("item-event-source", "item-event-source".getBytes()));
        return new ProducerRecord<>(topic, null, itemEvent.getEventId().toString(), itemEvent, requestHeader);
    }

    private void handleSuccess(SendResult<String, ItemEvent> result, ItemEvent event) {
        log.info("Message sent successfully: key : {}, value: {}, partition: {}", event.getEventId(), event, result.getRecordMetadata().partition());
    }

    private void handleFailure(Throwable ex, ItemEvent event) {
        log.error("Error while sending the message for: {} and the exception is: {}", event, ex.getMessage());
        try {
            throw ex;
        } catch (Throwable throwable) {
            log.error("Error in OnFailure: {}", throwable.getMessage());
        }
    }
}
