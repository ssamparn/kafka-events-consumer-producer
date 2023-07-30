package com.microservices.kafkaevents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record Item(

        @NotNull
        UUID itemId,

        @NotBlank
        String itemName,

        @NotBlank
        String itemOriginator
) {
}
