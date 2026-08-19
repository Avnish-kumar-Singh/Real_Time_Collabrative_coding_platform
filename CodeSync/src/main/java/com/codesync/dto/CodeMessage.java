package com.codesync.dto;

import java.util.List;
import java.util.Map;

public class CodeMessage {

    private MessageType type;
    private String content;
    private String username;
    private List<String> users;
    private String language;
    private String path;
    private List<String> paths;
    private Map<String, String> files;

    public CodeMessage() {
    }

    public CodeMessage(MessageType type, String content) {
        this.type = type;
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public void setFiles(Map<String, String> files) {
        this.files = files;
    }

    /** Sent once on connect: every file in the room's project, path -> content. */
    public static CodeMessage syncFiles(Map<String, String> files, String language) {
        CodeMessage message = new CodeMessage(MessageType.SYNC, null);
        message.setFiles(files);
        message.setLanguage(language);
        return message;
    }

    public static CodeMessage codeUpdate(String path, String content) {
        CodeMessage message = new CodeMessage(MessageType.CODE_UPDATE, content);
        message.setPath(path);
        return message;
    }

    public static CodeMessage error(String content) {
        return new CodeMessage(MessageType.ERROR, content);
    }

    public static CodeMessage userJoined(String username) {
        CodeMessage message = new CodeMessage(MessageType.USER_JOINED, null);
        message.setUsername(username);
        return message;
    }

    public static CodeMessage userLeft(String username) {
        CodeMessage message = new CodeMessage(MessageType.USER_LEFT, null);
        message.setUsername(username);
        return message;
    }

    public static CodeMessage presence(List<String> users) {
        CodeMessage message = new CodeMessage(MessageType.PRESENCE, null);
        message.setUsers(users);
        return message;
    }

    public static CodeMessage languageUpdate(String language) {
        CodeMessage message = new CodeMessage(MessageType.LANGUAGE_UPDATE, null);
        message.setLanguage(language);
        return message;
    }

    public static CodeMessage fileCreated(String path) {
        CodeMessage message = new CodeMessage(MessageType.FILE_CREATE, "");
        message.setPath(path);
        return message;
    }

    public static CodeMessage filesDeleted(List<String> paths) {
        CodeMessage message = new CodeMessage(MessageType.FILE_DELETE, null);
        message.setPaths(paths);
        return message;
    }
}
