package com.microservices.kafkaevents.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.kafkaevents.dto.Item;
import com.microservices.kafkaevents.dto.ItemEvent;
import com.microservices.kafkaevents.dto.ItemEventType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemEventsUtil {

    private static final String DEFAULT_NAME = "Harry Potter";
    private static final String DEFAULT_ORIGINATOR = "JK Rowling";

    // Item factory methods
    public static Item validItem() {
        return new Item(UUID.randomUUID(), DEFAULT_NAME, DEFAULT_ORIGINATOR);
    }

    public static Item invalidItem() {
        return new Item(null, "","Kafka Using Spring Boot" );
    }

    // ItemEvent factory methods
    public static ItemEvent createItemEvent() {
        return new ItemEvent(null, validItem(), ItemEventType.CREATE);
    }

    public static ItemEvent createItemEventWithEventId() {
        return new ItemEvent(UUID.randomUUID(), validItem(), ItemEventType.CREATE);
    }

    public static ItemEvent updateItemEvent() {
        return new ItemEvent(UUID.randomUUID(), validItem(), ItemEventType.UPDATE);
    }

    public static ItemEvent updateItemEventWithEventId(UUID eventId) {
        return new ItemEvent(eventId, validItem(), ItemEventType.UPDATE);
    }

    public static ItemEvent updateItemEventWithNullEventId() {
        return new ItemEvent(null,  validItem(), ItemEventType.UPDATE);
    }

    public static ItemEvent createItemEventWithInvalidItem() {
        return new ItemEvent(null, invalidItem(), ItemEventType.CREATE);
    }

    public static ItemEvent toItemEvent(ObjectMapper objectMapper, String json) {
        try {
            return objectMapper.readValue(json, ItemEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid ItemEvent JSON String", e);
        }
    }
}
