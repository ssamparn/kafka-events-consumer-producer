package com.microservices.kafkaeventproducer.service;

import com.microservices.kafkaevents.dto.ItemEvent;
import com.microservices.kafkaeventproducer.producer.ItemEventsProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemEventsService {

    private final ItemEventsProducer eventsProducer;

    public ItemEvent createNewItem(ItemEvent itemEvent) {
        itemEvent.setEventId(UUID.randomUUID());
        this.eventsProducer.sendMessage(itemEvent);

        return itemEvent;
    }
}
