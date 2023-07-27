package com.microservices.kafkaeventconsumer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ItemEntity {

    @Id
    private UUID itemId;
    private String itemName;
    private String itemOriginator;

    @OneToOne
    @JoinColumn(name = "eventId")
    private ItemEventEntity itemEvent;
}
