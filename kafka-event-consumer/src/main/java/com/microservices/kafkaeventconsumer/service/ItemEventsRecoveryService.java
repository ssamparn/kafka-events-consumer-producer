package com.microservices.kafkaeventconsumer.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Slf4j
@Service
public class ItemEventsRecoveryService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void handleRecovery(ConsumerRecord<String, String> consumerRecord) {
        String key = consumerRecord.key();
        String value = consumerRecord.value();

        ListenableFuture<SendResult<String, String>> listenableFuture = kafkaTemplate.sendDefault(key, value);
        listenableFuture.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onFailure(Throwable ex) {
                handleFailure(key, value, ex);
            }
            @Override
            public void onSuccess(SendResult<String, String> result) {
                handleSuccess(key, value, result);
            }
        });
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
