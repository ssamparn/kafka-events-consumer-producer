package com.microservices.kafkaeventconsumer.consumer;

import com.microservices.kafkaeventconsumer.service.ItemEventsService;
import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component // initialize the bean only if you want to manage the offset automatically otherwise Load itemEventsConsumerManualOffset bean.
@RequiredArgsConstructor
public class ItemEventsConsumer {

    private final ItemEventsService itemEventsService;

    @KafkaListener(
            topics = "${spring.topics.default}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, ItemEvent> consumerRecord) {
        log.info("consumer record in direct listener: {}", consumerRecord);
        itemEventsService.processItemEvent(consumerRecord);
    }
}
