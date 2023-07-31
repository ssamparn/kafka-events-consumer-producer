package com.microservices.kafkaeventconsumer.consumer;

import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
//@Component // initialize the bean if you want to manage the offset manually. Otherwise, load itemEventsConsumer bean
public class ItemEventsConsumerManualOffset implements AcknowledgingMessageListener<String, ItemEvent> {

    @Override
    @KafkaListener(topics = "item-event-topic")
    public void onMessage(ConsumerRecord<String, ItemEvent> consumerRecord, Acknowledgment acknowledgment) {
        log.info("ConsumerRecord in Manual Offset Consumer: {} ", consumerRecord );
        acknowledgment.acknowledge();
    }
}
