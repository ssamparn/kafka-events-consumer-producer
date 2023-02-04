//package com.microservices.kafkaeventproducer.unit.producer;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.microservices.kafkaeventproducer.domain.Item;
//import com.microservices.kafkaeventproducer.domain.ItemEvent;
//import com.microservices.kafkaeventproducer.domain.ItemEventType;
//import com.microservices.kafkaeventproducer.producer.ItemEventsProducer;
//import org.apache.kafka.clients.producer.ProducerRecord;
//import org.apache.kafka.clients.producer.RecordMetadata;
//import org.apache.kafka.common.TopicPartition;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Spy;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.SendResult;
//import org.springframework.util.concurrent.SettableListenableFuture;
//
//import java.util.UUID;
//
//@ExtendWith(MockitoExtension.class)
//class ItemEventsProducerUnitTest {
//
//    private ItemEvent itemEvent;
//    private SettableListenableFuture listenableFuture;
//    private ProducerRecord<String, String> producerRecord;
//    private RecordMetadata recordMetadata;
//    private SendResult<String, String> sendResult;
//
//    @Mock private KafkaTemplate<String, String> kafkaTemplate;
//    @InjectMocks private ItemEventsProducer itemProducer;
//    @Spy private ObjectMapper objectMapper = new ObjectMapper();
//
//    @BeforeEach
//    void setUp() throws JsonProcessingException {
//        itemEvent = ItemEvent.builder()
//                .eventId(UUID.randomUUID())
//                .item(Item.builder()
//                        .itemId(UUID.randomUUID())
//                        .itemName("Harry Potter")
//                        .itemOriginator("JK Rowling")
//                        .build())
//                .itemEventType(ItemEventType.CREATE)
//                .build();
//
//        listenableFuture = new SettableListenableFuture();
//
//        producerRecord = new ProducerRecord<>("item-topic", itemEvent.getEventId().toString(), objectMapper.writeValueAsString(itemEvent));
//        recordMetadata = new RecordMetadata(new TopicPartition("item-topic", 1), 1, 1, 342, System.currentTimeMillis(), 1, 2);
//        sendResult = new SendResult<>(producerRecord, recordMetadata);
//    }
//
//    @Test
//    void sendItemEventAnotherApproachTest_OnSuccess() throws JsonProcessingException, ExecutionException, InterruptedException {
//        listenableFuture.set(sendResult);
//        when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(listenableFuture);
//
//        ListenableFuture<SendResult<String, String>> listenableFuture = itemProducer.sendItemEventAsyncAnotherApproach(itemEvent);
//
//        SendResult<String, String> result = listenableFuture.get();
//        assert result.getRecordMetadata().partition() == 1;
//    }
//
//    @Test
//    void sendItemEventAnotherApproachTest_OnFailure() {
//        listenableFuture.setException(new RuntimeException("Exception calling kafka broker"));
//        when(kafkaTemplate.send(isA(ProducerRecord.class))).thenReturn(listenableFuture);
//
//        Assertions.assertThrows(Exception.class,
//                () -> itemProducer.sendItemEventAsyncAnotherApproach(itemEvent).get()
//        );
//    }
//}