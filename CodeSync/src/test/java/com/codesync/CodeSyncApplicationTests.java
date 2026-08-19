package com.codesync;

import com.codesync.exception.RoomNotFoundException;
import com.codesync.model.Room;
import com.codesync.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CodeSyncApplicationTests {

    @Autowired
    private RoomService roomService;

    @Test
    void contextLoads() {
    }

    @Test
    void createAndFetchRoom() {
        Room room = roomService.createRoom();

        assertNotNull(room.getRoomId());
        assertEquals(8, room.getRoomId().length());
        assertEquals("", room.getCode());

        Room fetched = roomService.getRoom(room.getRoomId());
        assertEquals(room.getRoomId(), fetched.getRoomId());
    }

    @Test
    void updateCodePersistsInRoom() {
        Room room = roomService.createRoom();
        roomService.updateCode(room.getRoomId(), "console.log('hello');");

        assertEquals("console.log('hello');", roomService.getRoom(room.getRoomId()).getCode());
    }

    @Test
    void getMissingRoomThrowsNotFound() {
        assertThrows(RoomNotFoundException.class, () -> roomService.getRoom("missing1"));
    }
}
