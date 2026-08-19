package com.codesync.controller;

import com.codesync.dto.RoomSummary;
import com.codesync.model.Room;
import com.codesync.model.User;
import com.codesync.service.RoomFileService;
import com.codesync.service.RoomService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RoomController {

    private final RoomService roomService;
    private final RoomFileService roomFileService;

    public RoomController(RoomService roomService, RoomFileService roomFileService) {
        this.roomService = roomService;
        this.roomFileService = roomFileService;
    }

    @PostMapping("/room/create")
    public Room createRoom(Authentication authentication) {
        Long ownerId = extractUserId(authentication);
        return roomService.createRoom(ownerId);
    }

    @GetMapping("/room/{roomId}")
    public Room getRoom(@PathVariable String roomId) {
        return roomService.getRoom(roomId);
    }

    /** Rooms owned by the currently logged-in user, most recently updated first. */
    @GetMapping("/rooms/mine")
    public List<RoomSummary> myRooms(Authentication authentication) {
        Long userId = extractUserId(authentication);
        if (userId == null) {
            throw new BadCredentialsException("Login required");
        }

        return roomService.listRoomsForUser(userId).stream()
                .map(room -> new RoomSummary(room.getRoomId(), room.getUpdatedAt().toString(), preview(room.getRoomId())))
                .toList();
    }

    private String preview(String roomId) {
        int fileCount = roomFileService.listFiles(roomId).size();
        return fileCount == 1 ? "1 file" : fileCount + " files";
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user.getId();
    }
}
