package com.codesync.repository;

import com.codesync.model.RoomFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomFileRepository extends JpaRepository<RoomFile, Long> {

    List<RoomFile> findByRoomIdOrderByPath(String roomId);

    Optional<RoomFile> findByRoomIdAndPath(String roomId, String path);

    boolean existsByRoomIdAndPath(String roomId, String path);

    /** Matches the exact path, or anything nested under it as a "folder" (path/child). */
    @Query("SELECT f FROM RoomFile f WHERE f.roomId = :roomId AND (f.path = :path OR f.path LIKE CONCAT(:path, '/%'))")
    List<RoomFile> findExactOrNested(@Param("roomId") String roomId, @Param("path") String path);
}
