package com.microservices.kafkaevents.dto;

import java.util.UUID;

public record Item(
        UUID itemId,
        String itemName,
        String itemOriginator
) {
}
