package com.codesync.service;

import com.codesync.dto.AuthResponse;
import com.codesync.model.User;
import com.codesync.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, SessionService sessionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
    }

    public AuthResponse register(String username, String password) {
        validateCredentials(username, password);

        String normalized = username.trim();
        if (userRepository.existsByUsername(normalized)) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = new User();
        user.setUsername(normalized);
        user.setPasswordHash(passwordEncoder.encode(password));
        user = userRepository.save(user);

        String token = sessionService.createSession(user.getId());
        return new AuthResponse(token, user.getUsername());
    }

    public AuthResponse login(String username, String password) {
        if (username == null || password == null) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = sessionService.createSession(user.getId());
        return new AuthResponse(token, user.getUsername());
    }

    public void logout(String token) {
        if (token != null) {
            sessionService.invalidate(token);
        }
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
    }
}
