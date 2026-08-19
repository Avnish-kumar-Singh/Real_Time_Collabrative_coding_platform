package com.codesync.dto;

/**
 * WebRTC signaling envelope relayed between a screen-sharer and viewers.
 * type: PEER_JOINED | PEER_LEFT | OFFER | ANSWER | ICE_CANDIDATE
 * targetId: null for broadcast messages (PEER_JOINED/PEER_LEFT), a specific
 *           session id for point-to-point messages (OFFER/ANSWER/ICE_CANDIDATE).
 * payload: JSON-stringified SDP or ICE candidate (opaque to the server — never parsed).
 */
public class ScreenSignalMessage {

    private String type;
    private String senderId;
    private String targetId;
    private String payload;

    public ScreenSignalMessage() {
    }

    public ScreenSignalMessage(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public static ScreenSignalMessage peerJoined(String senderId) {
        ScreenSignalMessage message = new ScreenSignalMessage("PEER_JOINED", null);
        message.setSenderId(senderId);
        return message;
    }

    public static ScreenSignalMessage peerLeft(String senderId) {
        ScreenSignalMessage message = new ScreenSignalMessage("PEER_LEFT", null);
        message.setSenderId(senderId);
        return message;
    }
}
