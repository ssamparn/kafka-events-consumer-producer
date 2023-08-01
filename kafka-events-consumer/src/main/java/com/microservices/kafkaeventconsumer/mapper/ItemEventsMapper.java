package com.microservices.kafkaeventconsumer.mapper;

import com.microservices.kafkaeventconsumer.entity.ItemEntity;
import com.microservices.kafkaeventconsumer.entity.ItemEventEntity;
import com.microservices.kafkaeventconsumer.entity.ItemEventTypeEntity;
import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ItemEventsMapper {

    public ItemEventEntity mapToItemEventEntity(ItemEvent event) {
        log.info("itemEvent Received: {}", event);

        ItemEventEntity entity = new ItemEventEntity();
        entity.setEventId(event.getEventId());
        entity.setItem(ItemEntity.builder()
                .itemId(event.getItem().getItemId())
                .itemName(event.getItem().getItemName())
                .itemOriginator(event.getItem().getItemOriginator())
                .build());
        entity.setItemEventType(ItemEventTypeEntity.valueOf(event.getItemEventType().toString()));

        return entity;
    }
}
