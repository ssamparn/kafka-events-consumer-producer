package com.microservices.kafkaeventproducer.integration.controller;

import com.microservices.kafkaeventproducer.service.ItemEventsService;
import com.microservices.kafkaevents.dto.ItemEvent;
import com.microservices.kafkaevents.dto.ItemEventType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ItemEventsController {

    private final ItemEventsService itemEventsService;

    @PostMapping("/item-event")
    public ResponseEntity<ItemEvent> newItemEvent(@RequestBody @Valid ItemEvent itemEvent) {
        log.info("itemEvent: {}", itemEvent);

        itemEvent.setItemEventType(ItemEventType.CREATE);
        ItemEvent emittedItem = itemEventsService.createNewItem(itemEvent);

        return new ResponseEntity<>(emittedItem, HttpStatus.CREATED);
    }

    @PutMapping("/item-event")
    public ResponseEntity<?> updateItemEvent(@RequestBody @Valid ItemEvent itemEvent) {

        if (itemEvent.getEventId() == null) {
            return new ResponseEntity<>("Missing event Id", HttpStatus.BAD_REQUEST);
        }

        itemEvent.setItemEventType(ItemEventType.UPDATE);
        itemEventsService.createNewItem(itemEvent);

        return new ResponseEntity<>(itemEvent, HttpStatus.OK);
    }

}
