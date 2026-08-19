package com.codesync.service;

import com.codesync.exception.RoomNotFoundException;
import com.codesync.model.Room;
import com.codesync.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private static final int ROOM_ID_LENGTH = 8;
    private static final String DEFAULT_FILE_PATH = "main.js";

    private final RoomRepository roomRepository;
    private final RoomFileService roomFileService;

    public RoomService(RoomRepository roomRepository, RoomFileService roomFileService) {
        this.roomRepository = roomRepository;
        this.roomFileService = roomFileService;
    }

    /** Anonymous room creation (no owner) — kept for backward compatibility. */
    public Room createRoom() {
        return createRoom(null);
    }

    public Room createRoom(Long ownerId) {
        String roomId = generateRoomId();
        Room room = new Room(roomId, "");
        room.setOwnerId(ownerId);
        room = roomRepository.save(room);
        roomFileService.createFile(roomId, DEFAULT_FILE_PATH);
        return room;
    }

    public Room getRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    public boolean exists(String roomId) {
        return roomRepository.existsById(roomId);
    }

    public void updateCode(String roomId, String code) {
        Room room = getRoom(roomId);
        room.setCode(code);
        roomRepository.save(room);
    }

    public void updateLanguage(String roomId, String language) {
        Room room = getRoom(roomId);
        room.setLanguage(language);
        roomRepository.save(room);
    }

    public List<Room> listRoomsForUser(Long userId) {
        return roomRepository.findByOwnerIdOrderByUpdatedAtDesc(userId);
    }

    private String generateRoomId() {
        String roomId;
        do {
            roomId = UUID.randomUUID().toString().replace("-", "").substring(0, ROOM_ID_LENGTH);
        } while (roomRepository.existsById(roomId));
        return roomId;
    }
}
