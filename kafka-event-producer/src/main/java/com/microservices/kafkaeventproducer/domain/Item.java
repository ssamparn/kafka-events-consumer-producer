package com.microservices.kafkaeventproducer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @NotNull private UUID itemId;
    @NotBlank private String itemName;
    @NotBlank private String itemOriginator;
}
