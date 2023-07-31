package com.microservices.kafkaeventconsumer.repository;

import com.microservices.kafkaeventconsumer.entity.ItemEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ItemEventsRepository extends JpaRepository<ItemEventEntity, UUID> {

}
