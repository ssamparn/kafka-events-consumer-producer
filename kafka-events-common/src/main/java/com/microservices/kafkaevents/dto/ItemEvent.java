package com.microservices.kafkaevents.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ItemEvent(
        UUID eventId,

        @NotNull
        @Valid
        Item item,
        ItemEventType itemEventType
) {
}
