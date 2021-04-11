package com.microservices.kafkaeventconsumer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
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
