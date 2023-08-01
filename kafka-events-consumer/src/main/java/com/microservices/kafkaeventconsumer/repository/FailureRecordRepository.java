package com.microservices.kafkaeventconsumer.repository;

import com.microservices.kafkaeventconsumer.entity.FailureRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailureRecordRepository extends JpaRepository<FailureRecordEntity, Integer> {

}
