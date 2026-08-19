package com.codesync.service;

import com.codesync.model.RoomFile;
import com.codesync.repository.RoomFileRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoomFileService {

    private final RoomFileRepository roomFileRepository;

    public RoomFileService(RoomFileRepository roomFileRepository) {
        this.roomFileRepository = roomFileRepository;
    }

    public List<RoomFile> listFiles(String roomId) {
        return roomFileRepository.findByRoomIdOrderByPath(roomId);
    }

    /** path -> content, for sending the whole project to a client on connect. */
    public Map<String, String> asContentMap(String roomId) {
        Map<String, String> files = new LinkedHashMap<>();
        for (RoomFile file : listFiles(roomId)) {
            files.put(file.getPath(), file.getContent());
        }
        return files;
    }

    public RoomFile createFile(String roomId, String path) {
        String normalized = validateAndNormalize(path);
        if (roomFileRepository.existsByRoomIdAndPath(roomId, normalized)) {
            throw new IllegalArgumentException("A file already exists at: " + normalized);
        }
        return roomFileRepository.save(new RoomFile(roomId, normalized, ""));
    }

    public void updateContent(String roomId, String path, String content) {
        RoomFile file = roomFileRepository.findByRoomIdAndPath(roomId, path)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));
        file.setContent(content);
        roomFileRepository.save(file);
    }

    /** Deletes the file at path, or — if path is a "folder" — everything nested under it. Returns removed paths. */
    public List<String> deletePathOrFolder(String roomId, String path) {
        List<RoomFile> matches = roomFileRepository.findExactOrNested(roomId, path);
        List<String> removedPaths = matches.stream().map(RoomFile::getPath).toList();
        roomFileRepository.deleteAll(matches);
        return removedPaths;
    }

    private String validateAndNormalize(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path is required");
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("/") || trimmed.contains("..") || trimmed.contains("\\")) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        return trimmed;
    }
}
