package com.microservices.kafkaeventproducer.controller;

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
    public ResponseEntity<?> newItemEvent(@RequestBody @Valid ItemEvent itemEvent) {
        log.info("itemEvent to create: {}", itemEvent);

        if (ItemEventType.CREATE != itemEvent.getItemEventType()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Only CREATE event type is supported");
        }

        ItemEvent emittedItem = itemEventsService.createNewItem(itemEvent);

        return new ResponseEntity<>(emittedItem, HttpStatus.CREATED);
    }

    @PutMapping("/item-event")
    public ResponseEntity<?> updateItemEvent(@RequestBody @Valid ItemEvent itemEvent) {

        log.info("itemEvent to update: {}", itemEvent);

        ResponseEntity<String> BAD_REQUEST = validateItemEvent(itemEvent);
        if (BAD_REQUEST != null) return BAD_REQUEST;

        itemEventsService.updateItem(itemEvent);

        return new ResponseEntity<>(itemEvent, HttpStatus.OK);
    }

    private static ResponseEntity<String> validateItemEvent(ItemEvent itemEvent) {
        if (itemEvent.getEventId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please pass the itemEventId");
        }

        if (!ItemEventType.UPDATE.equals(itemEvent.getItemEventType()))  {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Only UPDATE event type is supported");
        }
        return null;
    }

}
