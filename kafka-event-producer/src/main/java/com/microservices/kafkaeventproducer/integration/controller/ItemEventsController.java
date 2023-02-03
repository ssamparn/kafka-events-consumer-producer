package com.microservices.kafkaeventproducer.integration.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microservices.kafkaeventproducer.domain.ItemEvent;
import com.microservices.kafkaeventproducer.domain.ItemEventType;
import com.microservices.kafkaeventproducer.producer.ItemEventProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ItemEventsController {

    private final ItemEventProducer producer;

    @PostMapping("/v1/item-event")
    public ResponseEntity<ItemEvent> createItem(@RequestBody @Valid ItemEvent itemEvent) throws JsonProcessingException {
        itemEvent.setItemEventType(ItemEventType.CREATE);
        producer.sendItemEventAsync(itemEvent);

        return new ResponseEntity<>(itemEvent, HttpStatus.CREATED);
    }

    @PutMapping("/v1/item-event")
    public ResponseEntity<?> updateItemEvent(@RequestBody @Valid ItemEvent itemEvent) throws JsonProcessingException {

        if (itemEvent.getEventId() == null) {
            return new ResponseEntity<>("Missing event Id", HttpStatus.BAD_REQUEST);
        }

        itemEvent.setItemEventType(ItemEventType.UPDATE);
        producer.sendItemEventAsync(itemEvent);

        return new ResponseEntity<>(itemEvent, HttpStatus.OK);
    }

}
