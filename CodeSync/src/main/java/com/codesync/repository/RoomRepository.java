package com.codesync.repository;

import com.codesync.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, String> {
    List<Room> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId);
}
