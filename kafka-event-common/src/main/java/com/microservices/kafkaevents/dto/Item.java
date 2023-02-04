package com.microservices.kafkaevents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @NotNull
    private UUID itemId;
    @NotBlank
    private String itemName;
    @NotBlank private String itemOriginator;
}
