package com.microservices.kafkaeventproducer.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.kafkaeventproducer.domain.Item;
import com.microservices.kafkaeventproducer.domain.ItemEvent;
import com.microservices.kafkaeventproducer.domain.ItemEventType;
import com.microservices.kafkaeventproducer.integration.controller.ItemEventsController;
import com.microservices.kafkaeventproducer.producer.ItemEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(ItemEventsController.class)
public class ItemEventsControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemEventProducer producer;

    @Test
    void postItemEvent() throws Exception {
        ItemEvent itemEvent = ItemEvent.builder()
                .eventId(null)
                .item(Item.builder()
                        .itemId(UUID.randomUUID())
                        .itemName("Harry Potter")
                        .itemOriginator("JK Rowling")
                        .build())
                .itemEventType(ItemEventType.CREATE)
                .build();

        String requestBody = objectMapper.writeValueAsString(itemEvent);

        when(producer.sendItemEventAsyncAnotherApproach(isA(ItemEvent.class))).thenReturn(null);

        mockMvc.perform(post("/v1/item-event")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void postItemEvent_BadRequest() throws Exception {
        ItemEvent itemEvent = ItemEvent.builder()
                .eventId(null)
                .item(null)
                .itemEventType(ItemEventType.CREATE)
                .build();

        String requestBody = objectMapper.writeValueAsString(itemEvent);

        when(producer.sendItemEventAsyncAnotherApproach(isA(ItemEvent.class))).thenReturn(null);

        mockMvc.perform(post("/v1/item-event")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
        .andExpect(content().string("item - must not be null"));
    }

    @Test
    void updateItemEvent() throws Exception {
        ItemEvent itemEvent = ItemEvent.builder()
                .eventId(UUID.randomUUID())
                .item(Item.builder()
                        .itemId(UUID.randomUUID())
                        .itemName("Harry Potter")
                        .itemOriginator("JK Rowling")
                        .build())
                .itemEventType(ItemEventType.CREATE)
                .build();

        String requestBody = objectMapper.writeValueAsString(itemEvent);

        when(producer.sendItemEventAsyncAnotherApproach(isA(ItemEvent.class))).thenReturn(null);

        mockMvc.perform(put("/v1/item-event")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

}
