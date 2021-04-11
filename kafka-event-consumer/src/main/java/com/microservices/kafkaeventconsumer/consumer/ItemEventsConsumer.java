package com.microservices.kafkaeventconsumer.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microservices.kafkaeventconsumer.service.ItemEventsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ItemEventsConsumer {

    @Autowired
    private ItemEventsService itemEventsService;

    @KafkaListener(topics = {"item-topic"})
    public void onMessage(ConsumerRecord<String, String> consumerRecord) throws JsonProcessingException {
        log.info("consumerRecord : {}", consumerRecord);

        itemEventsService.processItemEvent(consumerRecord);
    }
}
