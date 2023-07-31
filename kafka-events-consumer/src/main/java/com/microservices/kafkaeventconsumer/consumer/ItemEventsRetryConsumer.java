package com.microservices.kafkaeventconsumer.consumer;

import com.microservices.kafkaeventconsumer.service.ItemEventsService;
import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItemEventsRetryConsumer {

    private final ItemEventsService itemEventsService;

    @KafkaListener(
            topics = "${topics.retry}",
            autoStartup = "${retryListener.startup:true}",
            groupId = "retry-listener-group"
    )
    public void onMessage(ConsumerRecord<String, ItemEvent> consumerRecord) {
        log.info("consumer record in retry listener: {} ", consumerRecord );
        consumerRecord.headers()
                .forEach(header -> log.info("retried consumer record key: {}, value: {}", header.key(), new String(header.value())));
        itemEventsService.processItemEvent(consumerRecord);
    }
}
