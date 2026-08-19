package com.codesync.dto;

public class RoomSummary {

    private String roomId;
    private String updatedAt;
    private String preview;

    public RoomSummary() {
    }

    public RoomSummary(String roomId, String updatedAt, String preview) {
        this.roomId = roomId;
        this.updatedAt = updatedAt;
        this.preview = preview;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }
}
