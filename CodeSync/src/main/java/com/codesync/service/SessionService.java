package com.codesync.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory bearer-token session store.
 * Tokens are lost on server restart (users are logged out); accounts and rooms
 * themselves persist in the database regardless. Swap for a persisted/Redis-backed
 * session store later if "stay logged in across restarts" becomes a requirement.
 */
@Service
public class SessionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Long> tokenToUserId = new ConcurrentHashMap<>();

    public String createSession(Long userId) {
        String token = generateToken();
        tokenToUserId.put(token, userId);
        return token;
    }

    public Optional<Long> getUserId(String token) {
        return Optional.ofNullable(tokenToUserId.get(token));
    }

    public void invalidate(String token) {
        tokenToUserId.remove(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
