package com.microservices.kafkaeventproducer.integration.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microservices.kafkaeventproducer.domain.ItemEvent;
import com.microservices.kafkaeventproducer.domain.ItemEventType;
import com.microservices.kafkaeventproducer.producer.ItemEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.ExecutionException;

@RestController
@RequiredArgsConstructor
public class ItemEventsController {

    private final ItemEventProducer producer;

    @PostMapping("/v1/item-event")
    public ResponseEntity<ItemEvent> postItemEvent(@RequestBody @Valid ItemEvent itemEvent) throws JsonProcessingException, ExecutionException, InterruptedException {

//      Invoke Kafka Producer in an asynchronous way
//      itemEvent.setItemEventType(ItemEventType.CREATE);
//      producer.sendItemEventAsync(itemEvent);

        itemEvent.setItemEventType(ItemEventType.CREATE);
        producer.sendItemEventAsyncAnotherApproach(itemEvent);

//      Invoke Kafka Producer in a synchronous way
//      itemEvent.setItemEventType(ItemEventType.CREATE);
//      producer.sendItemEventSync(itemEvent);

        return new ResponseEntity<>(itemEvent, HttpStatus.CREATED);
    }

    @PutMapping("/v1/item-event")
    public ResponseEntity<?> updateItemEvent(@RequestBody @Valid ItemEvent itemEvent) throws JsonProcessingException {

        if (itemEvent.getEventId() == null) {
            return new ResponseEntity<>("Missing event Id", HttpStatus.BAD_REQUEST);
        }

        itemEvent.setItemEventType(ItemEventType.UPDATE);
        producer.sendItemEventAsyncAnotherApproach(itemEvent);

        return new ResponseEntity<>(itemEvent, HttpStatus.OK);
    }

}
