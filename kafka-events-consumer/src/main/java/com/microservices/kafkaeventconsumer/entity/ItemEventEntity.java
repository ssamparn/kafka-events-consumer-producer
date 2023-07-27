package com.microservices.kafkaeventconsumer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ItemEventEntity {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    private ItemEventTypeEntity itemEventType;

    @ToString.Exclude
    @OneToOne(mappedBy = "itemEvent", cascade = {CascadeType.ALL})
    private ItemEntity item;
}
