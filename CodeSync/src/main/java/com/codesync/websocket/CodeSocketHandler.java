package com.codesync.websocket;

import com.codesync.dto.CodeMessage;
import com.codesync.dto.MessageType;
import com.codesync.service.RoomFileService;
import com.codesync.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CodeSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CodeSocketHandler.class);
    private static final String USERNAME_ATTR = "username";

    private final RoomService roomService;
    private final RoomFileService roomFileService;
    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final AtomicInteger guestCounter = new AtomicInteger(1);

    public CodeSocketHandler(RoomService roomService, RoomFileService roomFileService, ObjectMapper objectMapper) {
        this.roomService = roomService;
        this.roomFileService = roomFileService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = getRoomId(session);

        if (!roomService.exists(roomId)) {
            log.warn("Rejected connection to unknown room: {}", roomId);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Room not found"));
            return;
        }

        String username = resolveUsername(session);
        session.getAttributes().put(USERNAME_ATTR, username);

        Set<WebSocketSession> sessions = roomSessions.computeIfAbsent(roomId, id -> ConcurrentHashMap.newKeySet());
        sessions.add(session);

        Map<String, String> files = roomFileService.asContentMap(roomId);
        String currentLanguage = roomService.getRoom(roomId).getLanguage();
        session.sendMessage(toTextMessage(CodeMessage.syncFiles(files, currentLanguage)));
        session.sendMessage(toTextMessage(CodeMessage.presence(currentUsernames(sessions))));

        broadcastToOthers(roomId, session, CodeMessage.userJoined(username));

        log.info("User '{}' joined room: {} (active sessions: {})", username, roomId, sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String roomId = getRoomId(session);
        CodeMessage incoming = objectMapper.readValue(message.getPayload(), CodeMessage.class);

        if (incoming.getType() == null) {
            session.sendMessage(toTextMessage(CodeMessage.error("Invalid message format")));
            return;
        }

        switch (incoming.getType()) {
            case CODE_UPDATE -> handleCodeUpdate(session, roomId, incoming);
            case LANGUAGE_UPDATE -> handleLanguageUpdate(session, roomId, incoming);
            case FILE_CREATE -> handleFileCreate(session, roomId, incoming);
            case FILE_DELETE -> handleFileDelete(session, roomId, incoming);
            default -> session.sendMessage(toTextMessage(CodeMessage.error("Unsupported message type")));
        }
    }

    private void handleCodeUpdate(WebSocketSession session, String roomId, CodeMessage incoming) throws Exception {
        if (incoming.getPath() == null || incoming.getContent() == null) {
            session.sendMessage(toTextMessage(CodeMessage.error("path and content are required")));
            return;
        }
        try {
            roomFileService.updateContent(roomId, incoming.getPath(), incoming.getContent());
        } catch (IllegalArgumentException e) {
            session.sendMessage(toTextMessage(CodeMessage.error(e.getMessage())));
            return;
        }
        broadcastToOthers(roomId, session, CodeMessage.codeUpdate(incoming.getPath(), incoming.getContent()));
    }

    private void handleLanguageUpdate(WebSocketSession session, String roomId, CodeMessage incoming) throws Exception {
        if (incoming.getLanguage() == null) {
            session.sendMessage(toTextMessage(CodeMessage.error("language is required")));
            return;
        }
        roomService.updateLanguage(roomId, incoming.getLanguage());
        broadcastToOthers(roomId, session, CodeMessage.languageUpdate(incoming.getLanguage()));
    }

    private void handleFileCreate(WebSocketSession session, String roomId, CodeMessage incoming) throws Exception {
        if (incoming.getPath() == null || incoming.getPath().isBlank()) {
            session.sendMessage(toTextMessage(CodeMessage.error("path is required")));
            return;
        }
        try {
            roomFileService.createFile(roomId, incoming.getPath());
        } catch (IllegalArgumentException e) {
            session.sendMessage(toTextMessage(CodeMessage.error(e.getMessage())));
            return;
        }
        // Broadcast to everyone including the creator, so all clients stay in lockstep.
        broadcastToAll(roomId, CodeMessage.fileCreated(incoming.getPath()));
    }

    private void handleFileDelete(WebSocketSession session, String roomId, CodeMessage incoming) throws Exception {
        if (incoming.getPath() == null || incoming.getPath().isBlank()) {
            session.sendMessage(toTextMessage(CodeMessage.error("path is required")));
            return;
        }
        List<String> removed = roomFileService.deletePathOrFolder(roomId, incoming.getPath());
        if (removed.isEmpty()) {
            session.sendMessage(toTextMessage(CodeMessage.error("Nothing to delete at: " + incoming.getPath())));
            return;
        }
        broadcastToAll(roomId, CodeMessage.filesDeleted(removed));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = getRoomId(session);
        String username = (String) session.getAttributes().get(USERNAME_ATTR);
        Set<WebSocketSession> sessions = roomSessions.get(roomId);

        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
            } else if (username != null) {
                broadcastToOthers(roomId, session, CodeMessage.userLeft(username));
            }
            log.info("User '{}' left room: {} (remaining sessions: {})", username, roomId, sessions.size());
        }
    }

    private void broadcastToOthers(String roomId, WebSocketSession sender, CodeMessage message) throws Exception {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return;
        }

        TextMessage textMessage = toTextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen() && !session.getId().equals(sender.getId())) {
                session.sendMessage(textMessage);
            }
        }
    }

    private void broadcastToAll(String roomId, CodeMessage message) throws Exception {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return;
        }

        TextMessage textMessage = toTextMessage(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(textMessage);
            }
        }
    }

    private List<String> currentUsernames(Set<WebSocketSession> sessions) {
        List<String> usernames = new ArrayList<>();
        for (WebSocketSession session : sessions) {
            Object username = session.getAttributes().get(USERNAME_ATTR);
            if (username != null) {
                usernames.add((String) username);
            }
        }
        return usernames;
    }

    private TextMessage toTextMessage(CodeMessage message) throws Exception {
        return new TextMessage(objectMapper.writeValueAsString(message));
    }

    private String getRoomId(WebSocketSession session) {
        return getQueryParam(session, "roomId");
    }

    private String resolveUsername(WebSocketSession session) {
        String username = getQueryParam(session, "username");
        if (username == null || username.isBlank()) {
            return "Guest-" + guestCounter.getAndIncrement();
        }
        return username.trim();
    }

    private String getQueryParam(WebSocketSession session, String key) {
        if (session.getUri() == null) {
            return "";
        }

        String value = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst(key);

        if (value == null) {
            return "";
        }

        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
