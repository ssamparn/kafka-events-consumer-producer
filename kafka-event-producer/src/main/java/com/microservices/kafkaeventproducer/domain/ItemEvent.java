package com.microservices.kafkaeventproducer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemEvent {

    private UUID eventId;
    private ItemEventType itemEventType;
    @NotNull @Valid private Item item;
}
