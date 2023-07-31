package com.microservices.kafkaevents.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemEvent {

    private UUID eventId;

    @NotNull
    @Valid
    private Item item;

    private ItemEventType itemEventType;
}
