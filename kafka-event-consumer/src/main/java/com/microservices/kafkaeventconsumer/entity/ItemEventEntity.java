package com.microservices.kafkaeventconsumer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
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
