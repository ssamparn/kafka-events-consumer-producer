package com.microservices.kafkaeventproducer.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.kafkaeventproducer.domain.ItemEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class ItemEventProducer {

    private final static String topic = "item-topic";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendItemEventAsync(ItemEvent event) throws JsonProcessingException {

        String key = String.valueOf(event.getEventId());
        String value = objectMapper.writeValueAsString(event);

        CompletableFuture<SendResult<String, String>> sendResultCompletableFuture = kafkaTemplate.sendDefault(key, value);
        sendResultCompletableFuture.whenComplete((result, ex) -> {
            if (ex == null) {
                handleSuccess(key, value, result);
            } else {
                handleFailure(key, value, ex);
            }
        });
    }

    public SendResult<String, String> sendItemEventSync(ItemEvent event) throws JsonProcessingException, ExecutionException, InterruptedException {

        String key = String.valueOf(event.getEventId());
        String value = objectMapper.writeValueAsString(event);
        SendResult<String, String> sendResult;
        try {
            sendResult = kafkaTemplate.sendDefault(key, value).get();
        } catch (ExecutionException | InterruptedException e) {
            log.error("ExecutionException | InterruptedException while sending the message: {}", e.getMessage());
            throw e;
        } catch (Exception ex) {
            log.error("Exception while sending the message: {}", ex.getMessage());
            throw ex;
        }
        return sendResult;
    }

    private ProducerRecord<String, String> buildProducerRecord(String key, String value, String topic) {
        List<Header> recordHeaders = List.of(new RecordHeader("event-source", "postman-client".getBytes()));
        return new ProducerRecord<>(topic, null, key, value, recordHeaders);
    }

    private void handleFailure(String key, String value, Throwable ex) {
        log.error("Error while sending the message: {}", ex.getMessage());
        try {
            throw ex;
        } catch (Throwable throwable) {
            log.error("Error in OnFailure: {}", throwable.getMessage());
        }
    }

    private void handleSuccess(String key, String value, SendResult<String, String> result) {
        log.info("Message sent successfully: key : {}, value: {}, partition: {}", key, value, result.getRecordMetadata().partition());
    }
}
