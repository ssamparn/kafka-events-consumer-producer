package com.microservices.kafkaeventconsumer.service;

import com.microservices.kafkaeventconsumer.entity.ItemEntity;
import com.microservices.kafkaeventconsumer.entity.ItemEventEntity;
import com.microservices.kafkaeventconsumer.entity.ItemEventTypeEntity;
import com.microservices.kafkaeventconsumer.repository.ItemEventsRepository;
import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemEventsService {

    private final ItemEventsRepository itemEventsRepository;

    public void processItemEvent(ConsumerRecord<String, ItemEvent> consumerRecord) {
        ItemEventEntity inputItemEvent = mapToItemEventEntity(consumerRecord.value());
        log.info("itemEventEntity: {}", inputItemEvent);

        switch (inputItemEvent.getItemEventType()) {
            case CREATE -> {
                inputItemEvent.getItem().setItemEvent(inputItemEvent);
                ItemEventEntity savedItemEvent = itemEventsRepository.save(inputItemEvent);
                log.info("savedItemEvent : {}", savedItemEvent);
            }

            case UPDATE -> {
                Optional<ItemEventEntity> itemEventEntityOptional = getItemEventEntity(inputItemEvent);
                ItemEventEntity toBeUpdatedItemEntity = updatedItemEvent(inputItemEvent, itemEventEntityOptional);
                ItemEventEntity updatedItemEvent = itemEventsRepository.save(toBeUpdatedItemEntity);
                log.info("updatedItemEvent : {}", updatedItemEvent);
            }

            default -> log.info("Invalid Item Event Type");
        }
    }

    private ItemEventEntity mapToItemEventEntity(ItemEvent event) {
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

    private ItemEventEntity updatedItemEvent(ItemEventEntity inputItemEvent, Optional<ItemEventEntity> itemEventEntityOptional) {
        ItemEventEntity itemEventEntity = itemEventEntityOptional.get();

        itemEventEntity.setEventId(inputItemEvent.getEventId());
        itemEventEntity.setItemEventType(inputItemEvent.getItemEventType());
        itemEventEntity.setItem(ItemEntity.builder()
                .itemId(inputItemEvent.getItem().getItemId())
                .itemName(inputItemEvent.getItem().getItemName())
                .itemOriginator(inputItemEvent.getItem().getItemOriginator())
                .build());

        return itemEventEntity;
    }

    private Optional<ItemEventEntity> getItemEventEntity(ItemEventEntity itemEvent) {
        if (itemEvent.getEventId() == null) {
            throw new RecoverableDataAccessException("Missing item event id");
        }

        Optional<ItemEventEntity> itemEventEntityOptional = itemEventsRepository.findById(itemEvent.getEventId());
        if (itemEventEntityOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid item event");
        }
        log.info("Validation is successful for the item event: {}, ", itemEventEntityOptional.get());
        return itemEventEntityOptional;
    }
}
