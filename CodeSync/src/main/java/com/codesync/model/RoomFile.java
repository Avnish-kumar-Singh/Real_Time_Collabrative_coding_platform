package com.codesync.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * A single file within a room's project tree. There's no separate "folder"
 * entity — folders are implied by "/" in a path (e.g. "utils/helper.js"
 * implies a "utils" folder), the same convention git and most file trees use.
 */
@Entity
@Table(name = "room_files", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "path"}))
public class RoomFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String path;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String content;

    private Instant updatedAt;

    public RoomFile() {
    }

    public RoomFile(String roomId, String path, String content) {
        this.roomId = roomId;
        this.path = path;
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
