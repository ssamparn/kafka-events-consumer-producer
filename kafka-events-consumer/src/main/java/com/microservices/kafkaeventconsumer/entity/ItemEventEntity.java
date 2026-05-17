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
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ItemEventEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID eventId;

    @Version
    private Integer version;

    @Enumerated(EnumType.STRING)
    private ItemEventTypeEntity itemEventType;

    @ToString.Exclude
    @OneToOne(mappedBy = "itemEvent", cascade = {CascadeType.ALL})
    private ItemEntity item;
}
