package com.microservices.kafkaeventconsumer.service;

import com.microservices.kafkaeventconsumer.entity.ItemEntity;
import com.microservices.kafkaeventconsumer.entity.ItemEventEntity;
import com.microservices.kafkaeventconsumer.entity.ItemEventTypeEntity;
import com.microservices.kafkaeventconsumer.mapper.ItemEventsMapper;
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

    private final ItemEventsMapper itemEventsMapper;
    private final ItemEventsRepository itemEventsRepository;

    public void processItemEvent(ConsumerRecord<String, ItemEvent> consumerRecord) {
        ItemEventEntity inputItemEvent = itemEventsMapper.mapToItemEventEntity(consumerRecord.value());
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
