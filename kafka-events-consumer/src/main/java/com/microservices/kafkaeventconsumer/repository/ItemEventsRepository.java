package com.microservices.kafkaeventconsumer.repository;

import com.microservices.kafkaeventconsumer.entity.ItemEventEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ItemEventsRepository extends CrudRepository<ItemEventEntity, UUID> {

}
