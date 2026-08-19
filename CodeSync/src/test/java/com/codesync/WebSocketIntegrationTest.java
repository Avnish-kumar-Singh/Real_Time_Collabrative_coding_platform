package com.codesync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void receivesSyncOnJoinAndPersistsCodeUpdates() throws Exception {
        JsonNode created = objectMapper.readTree(
                java.net.http.HttpClient.newHttpClient().send(
                        java.net.http.HttpRequest.newBuilder()
                                .uri(URI.create("http://localhost:" + port + "/room/create"))
                                .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                                .build(),
                        java.net.http.HttpResponse.BodyHandlers.ofString()
                ).body()
        );

        String roomId = created.get("roomId").asText();
        CompletableFuture<String> firstMessage = new CompletableFuture<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                firstMessage.complete(message.getPayload());
            }
        }, null, URI.create("ws://localhost:" + port + "/code?roomId=" + roomId)).get(5, TimeUnit.SECONDS);

        // First message on join is a SYNC of the whole project: every room starts
        // with one default file, "main.js", empty.
        JsonNode sync = objectMapper.readTree(firstMessage.get(5, TimeUnit.SECONDS));
        assertEquals("SYNC", sync.get("type").asText());
        assertEquals("", sync.get("files").get("main.js").asText());

        session.sendMessage(new TextMessage(
                objectMapper.writeValueAsString(
                        java.util.Map.of("type", "CODE_UPDATE", "path", "main.js", "content", "console.log('live');")
                )
        ));

        Thread.sleep(300);

        // Reconnect (a second "client") and confirm the SYNC now reflects the update.
        CompletableFuture<String> secondJoinSync = new CompletableFuture<>();
        WebSocketSession secondSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                secondJoinSync.complete(message.getPayload());
            }
        }, null, URI.create("ws://localhost:" + port + "/code?roomId=" + roomId)).get(5, TimeUnit.SECONDS);

        JsonNode resync = objectMapper.readTree(secondJoinSync.get(5, TimeUnit.SECONDS));
        assertEquals("console.log('live');", resync.get("files").get("main.js").asText());

        assertTrue(session.isOpen());
        session.close();
        secondSession.close();
    }
}
