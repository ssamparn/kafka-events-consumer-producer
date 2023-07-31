package com.microservices.kafkaeventconsumer.consumer;

import com.microservices.kafkaeventconsumer.service.ItemEventsService;
import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component // initialize the bean only if you want to manage the offset automatically otherwise Load itemEventsConsumerManualOffset bean.
public class ItemEventsConsumer {

    @Autowired
    private ItemEventsService itemEventsService;

    @KafkaListener(
            topics = "item-event-topic",
            groupId = "item-events-listener-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, ItemEvent> consumerRecord) {
        log.info("consumerRecord : {}", consumerRecord);

        itemEventsService.processItemEvent(consumerRecord);
    }
}
