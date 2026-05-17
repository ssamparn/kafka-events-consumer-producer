package com.microservices.kafkaeventproducer.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.kafkaeventproducer.service.ItemEventsService;
import com.microservices.kafkaeventproducer.util.TestUtil;
import com.microservices.kafkaeventproducer.web.controller.ItemEventsController;
import com.microservices.kafkaevents.dto.ItemEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
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

    private ObjectMapper objectMapper;

    @MockitoBean
    private ItemEventsService itemEventsService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void postItemEvent() throws Exception {
        ItemEvent itemEvent = TestUtil.itemEventRecord();
        String requestBody = objectMapper.writeValueAsString(itemEvent);

        when(itemEventsService.createNewItem(any(ItemEvent.class))).thenReturn(null);

        mockMvc.perform(post("/api/v1/item-event")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void postItemEvent_4xx_BadRequest() throws Exception {
        ItemEvent invalidItemEvent = TestUtil.itemEventRecordWithInvalidItem();

        String requestBody = objectMapper.writeValueAsString(invalidItemEvent);

        when(itemEventsService.createNewItem(isA(ItemEvent.class))).thenReturn(null);

        mockMvc.perform(post("/api/v1/item-event")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
        .andExpect(content().string("item.itemId - must not be null, item.itemName - must not be blank"));
    }

    @Test
    void updateItemEvent() throws Exception {
        ItemEvent itemEvent = TestUtil.itemEventRecordUpdate();

        String requestBody = objectMapper.writeValueAsString(itemEvent);

        when(itemEventsService.createNewItem(isA(ItemEvent.class))).thenReturn(null);

        mockMvc.perform(put("/api/v1/item-event")
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void updateItemEvent_withNullItemEventId() throws Exception {
        ItemEvent itemEvent = TestUtil.itemEventRecordUpdateWithNullItemEventId();

        String requestBody = objectMapper.writeValueAsString(itemEvent);

        when(itemEventsService.createNewItem(isA(ItemEvent.class))).thenReturn(null);

        mockMvc.perform(put("/api/v1/item-event")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string("Please pass the itemEventId"));
    }

    @Test
    void updateItemEvent_withNullInvalidItemEventId() throws Exception {
        ItemEvent itemEvent = TestUtil.newItemEventRecordWithItemEventId();

        String requestBody = objectMapper.writeValueAsString(itemEvent);

        when(itemEventsService.createNewItem(isA(ItemEvent.class))).thenReturn(null);

        mockMvc.perform(put("/api/v1/item-event")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string("Only UPDATE event type is supported"));
    }
}
