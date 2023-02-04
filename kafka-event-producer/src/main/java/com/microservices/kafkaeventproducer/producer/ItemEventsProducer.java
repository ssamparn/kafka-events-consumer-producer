package com.microservices.kafkaeventproducer.producer;

import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class ItemEventsProducer {

    @Value("${spring.kafka.template.default-topic}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, ItemEvent> kafkaTemplate;

    public void sendMessage(ItemEvent itemEvent) {
        ProducerRecord<String, ItemEvent> producerRecord = new ProducerRecord<>(topic, itemEvent.getEventId().toString(), itemEvent);

        CompletableFuture<SendResult<String, ItemEvent>> sendResultCompletableFuture = kafkaTemplate.send(producerRecord);
        sendResultCompletableFuture.whenComplete((successResult, exception) -> {
            if (exception == null) {
                handleSuccess(successResult, itemEvent);
            } else {
                handleFailure(exception, itemEvent);
            }
        });
    }

    private void handleSuccess(SendResult<String, ItemEvent> result, ItemEvent event) {
        log.info("Message sent successfully: key : {}, value: {}, partition: {}", event.getEventId(), event, result.getRecordMetadata().partition());
    }

    private void handleFailure(Throwable ex, ItemEvent event) {
        log.error("Error while sending the message for {} and the exception is {}", event, ex);
        try {
            throw ex;
        } catch (Throwable throwable) {
            log.error("Error in OnFailure: {}", throwable.getMessage());
        }
    }
}
