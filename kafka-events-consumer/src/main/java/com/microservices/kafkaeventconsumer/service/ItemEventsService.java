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
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemEventsService {

    private final ItemEventsRepository itemEventsRepository;

    @Transactional
    public void processItemEvent(ConsumerRecord<String, ItemEvent> consumerRecord) {
        ItemEventEntity inputItemEvent = mapToItemEventEntity(consumerRecord.value());
        log.info("itemEventEntity: {}", inputItemEvent);

        // code block to simulate not to retry on exceptions
        if (inputItemEvent.getEventId() != null && inputItemEvent.getEventId().toString().equals("b9c21087-3391-46d4-91b7-5b493c057089")) {
            throw new RecoverableDataAccessException("Temporary Network Issue");
        }

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
        entity.setEventId(event.eventId());
        entity.setItem(ItemEntity.builder()
                        .itemId(event.item().itemId())
                        .itemName(event.item().itemName())
                        .itemOriginator(event.item().itemOriginator())
                .build());
        entity.setItemEventType(ItemEventTypeEntity.valueOf(event.itemEventType().toString()));

        return entity;
    }

    private ItemEventEntity updatedItemEvent(ItemEventEntity inputItemEvent, Optional<ItemEventEntity> itemEventEntityOptional) {
        ItemEventEntity itemEventEntity = itemEventEntityOptional.get();

        itemEventEntity.setItemEventType(inputItemEvent.getItemEventType());
        
        ItemEntity existingItem = itemEventEntity.getItem();
        if (existingItem != null) {
            existingItem.setItemName(inputItemEvent.getItem().getItemName());
            existingItem.setItemOriginator(inputItemEvent.getItem().getItemOriginator());
        } else {
            ItemEntity newItem = inputItemEvent.getItem();
            newItem.setItemEvent(itemEventEntity);
            itemEventEntity.setItem(newItem);
        }

        return itemEventEntity;
    }

    private Optional<ItemEventEntity> getItemEventEntity(ItemEventEntity itemEvent) {
        if (itemEvent.getEventId() == null) {
            throw new IllegalArgumentException("Missing item event id");
        }

        Optional<ItemEventEntity> itemEventEntityOptional = itemEventsRepository.findById(itemEvent.getEventId());
        if (itemEventEntityOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid item event");
        }
        log.info("Validation is successful for the item event: {}, ", itemEventEntityOptional.get());
        return itemEventEntityOptional;
    }
}
