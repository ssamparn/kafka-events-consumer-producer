package com.microservices.kafkaeventconsumer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.kafkaeventconsumer.entity.ItemEntity;
import com.microservices.kafkaeventconsumer.entity.ItemEventEntity;
import com.microservices.kafkaeventconsumer.repository.ItemEventsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class ItemEventsService {

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private ItemEventsRepository itemEventsRepository;

    public void processItemEvent(ConsumerRecord<String, String> consumerRecord) throws JsonProcessingException {
        ItemEventEntity inputItemEvent = objectMapper.readValue(consumerRecord.value(), ItemEventEntity.class);
        log.info("itemEventEntity: {}", inputItemEvent);

        switch (inputItemEvent.getItemEventType()) {
            case CREATE:
                inputItemEvent.getItem().setItemEvent(inputItemEvent);
                ItemEventEntity savedItemEvent = itemEventsRepository.save(inputItemEvent);
                log.info("savedItemEvent : {}", savedItemEvent);
                break;
            case UPDATE:
                Optional<ItemEventEntity> itemEventEntityOptional = getItemEventEntity(inputItemEvent);
                ItemEventEntity toBeUpdatedItemEntity = updatedItemEvent(inputItemEvent, itemEventEntityOptional);
                ItemEventEntity updatedItemEvent = itemEventsRepository.save(toBeUpdatedItemEntity);
                log.info("updatedItemEvent : {}", updatedItemEvent);
                break;
            default:
                log.info("Invalid Item Event Type");
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
