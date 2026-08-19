package com.codesync.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    private String roomId;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String code;

    private String language = "javascript";

    /** Nullable: rooms created by anonymous (not logged in) users have no owner. */
    private Long ownerId;

    private Instant createdAt;
    private Instant updatedAt;

    public Room() {
    }

    public Room(String roomId, String code) {
        this.roomId = roomId;
        this.code = code;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        this.updatedAt = Instant.now();
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
