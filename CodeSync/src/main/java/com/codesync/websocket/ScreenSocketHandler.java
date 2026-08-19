package com.codesync.websocket;

import com.codesync.dto.ScreenSignalMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure signaling relay for WebRTC screen sharing. This server never touches
 * video/audio data itself — it only forwards small JSON handshake messages
 * (SDP offers/answers, ICE candidates) between browsers so they can establish
 * a direct peer-to-peer connection. Rooms are ephemeral (in memory only,
 * no persistence) since a live video stream has nothing to save.
 */
@Component
public class ScreenSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ScreenSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, WebSocketSession>> shareRooms = new ConcurrentHashMap<>();

    public ScreenSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String shareRoomId = getShareRoomId(session);
        if (shareRoomId.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("shareRoomId required"));
            return;
        }

        Map<String, WebSocketSession> peers = shareRooms.computeIfAbsent(shareRoomId, id -> new ConcurrentHashMap<>());
        peers.put(session.getId(), session);

        broadcast(shareRoomId, session.getId(), ScreenSignalMessage.peerJoined(session.getId()));
        log.info("Peer {} joined screen-share room {} ({} peers)", session.getId(), shareRoomId, peers.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String shareRoomId = getShareRoomId(session);
        Map<String, WebSocketSession> peers = shareRooms.get(shareRoomId);
        if (peers == null) {
            return;
        }

        ScreenSignalMessage incoming = objectMapper.readValue(message.getPayload(), ScreenSignalMessage.class);
        incoming.setSenderId(session.getId());

        if (incoming.getTargetId() != null) {
            WebSocketSession target = peers.get(incoming.getTargetId());
            if (target != null && target.isOpen()) {
                target.sendMessage(toTextMessage(incoming));
            }
        } else {
            broadcast(shareRoomId, session.getId(), incoming);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String shareRoomId = getShareRoomId(session);
        Map<String, WebSocketSession> peers = shareRooms.get(shareRoomId);

        if (peers != null) {
            peers.remove(session.getId());
            if (peers.isEmpty()) {
                shareRooms.remove(shareRoomId);
            } else {
                broadcast(shareRoomId, session.getId(), ScreenSignalMessage.peerLeft(session.getId()));
            }
            log.info("Peer {} left screen-share room {} ({} peers remain)", session.getId(), shareRoomId, peers.size());
        }
    }

    private void broadcast(String shareRoomId, String excludeSessionId, ScreenSignalMessage message) throws Exception {
        Map<String, WebSocketSession> peers = shareRooms.get(shareRoomId);
        if (peers == null) {
            return;
        }

        TextMessage text = toTextMessage(message);
        for (WebSocketSession session : peers.values()) {
            if (session.isOpen() && !session.getId().equals(excludeSessionId)) {
                session.sendMessage(text);
            }
        }
    }

    private TextMessage toTextMessage(ScreenSignalMessage message) throws Exception {
        return new TextMessage(objectMapper.writeValueAsString(message));
    }

    private String getShareRoomId(WebSocketSession session) {
        if (session.getUri() == null) {
            return "";
        }
        String value = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("shareRoomId");
        return value != null ? value : "";
    }
}
