package com.codesync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createRoomReturnsRoomId() throws Exception {
        mockMvc.perform(post("/room/create"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").isNotEmpty())
                .andExpect(jsonPath("$.code").value(""));
    }

    @Test
    void getExistingRoomReturnsRoom() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/room/create"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode room = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String roomId = room.get("roomId").asText();

        mockMvc.perform(get("/room/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId));
    }

    @Test
    void getMissingRoomReturns404() throws Exception {
        mockMvc.perform(get("/room/notfound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Room not found: notfound"));
    }
}
