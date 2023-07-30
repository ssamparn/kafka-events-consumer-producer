package com.microservices.kafkaeventproducer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.kafkaevents.dto.Item;
import com.microservices.kafkaevents.dto.ItemEvent;
import com.microservices.kafkaevents.dto.ItemEventType;

import java.util.UUID;

public class TestUtil {

    public static Item itemRecord(){
        return new Item(UUID.randomUUID(), "Harry Potter","JK Rowling" );
    }

    public static Item itemRecordWithInvalidValues() {
        return new Item(null, "","Kafka Using Spring Boot" );
    }

    public static ItemEvent itemEventRecord() {
        return new ItemEvent(null, itemRecord(), ItemEventType.CREATE);
    }

    public static ItemEvent newItemEventRecordWithItemEventId(){
        return new ItemEvent(UUID.randomUUID(), itemRecord(), ItemEventType.CREATE);
    }

    public static ItemEvent itemEventRecordUpdate(){
        return new ItemEvent(UUID.randomUUID(), itemRecord(), ItemEventType.UPDATE);
    }

    public static ItemEvent itemEventRecordUpdateWithNullItemEventId(){
        return new ItemEvent(null,  itemRecord(), ItemEventType.UPDATE);
    }

    public static ItemEvent itemEventRecordWithInvalidBook(){
        return new ItemEvent(null, itemRecordWithInvalidValues(), ItemEventType.CREATE);
    }

    public static ItemEvent parseItemEventRecord(ObjectMapper objectMapper , String json) {

        try {
            return objectMapper.readValue(json, ItemEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
    
}
