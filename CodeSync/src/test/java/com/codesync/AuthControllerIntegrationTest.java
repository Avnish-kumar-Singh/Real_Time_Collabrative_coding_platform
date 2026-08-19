package com.codesync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueUsername() {
        return "user" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void registerThenLoginReturnsToken() throws Exception {
        String username = uniqueUsername();
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", "password123"));

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        String username = uniqueUsername();
        String registerBody = objectMapper.writeValueAsString(Map.of("username", username, "password", "password123"));
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk());

        String wrongBody = objectMapper.writeValueAsString(Map.of("username", username, "password", "wrongpass"));
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrongBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createdRoomIsListedUnderMyRooms() throws Exception {
        String username = uniqueUsername();
        String registerBody = objectMapper.writeValueAsString(Map.of("username", username, "password", "password123"));

        MvcResult registerResult = mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode auth = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String token = auth.get("token").asText();
        assertNotNull(token);

        MvcResult createResult = mockMvc.perform(post("/room/create").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode room = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String roomId = room.get("roomId").asText();

        mockMvc.perform(get("/rooms/mine").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value(roomId));
    }

    @Test
    void myRoomsWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/rooms/mine"))
                .andExpect(status().isUnauthorized());
    }
}
