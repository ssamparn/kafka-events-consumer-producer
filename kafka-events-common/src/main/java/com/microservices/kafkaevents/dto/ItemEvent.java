package com.microservices.kafkaevents.dto;

import java.util.UUID;

public record ItemEvent(
        UUID eventId,
        Item item,
        ItemEventType itemEventType
) {
}
