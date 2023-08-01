package com.microservices.kafkaeventconsumer.service;

import com.microservices.kafkaeventconsumer.entity.FailureRecordEntity;
import com.microservices.kafkaeventconsumer.repository.FailureRecordRepository;
import com.microservices.kafkaevents.dto.ItemEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FailureService {

    private final FailureRecordRepository failureRecordRepository;

    public void saveFailureRecord(ConsumerRecord<String, ItemEvent> record, Exception exception, String recordStatus) {
        FailureRecordEntity failureRecordEntity = new FailureRecordEntity(null, record.topic(), record.key(), record.value().toString(), record.partition(), record.offset(),
                exception.getCause().getMessage(),
                recordStatus);

        failureRecordRepository.save(failureRecordEntity);
    }
}
