package com.codesync.security;

import com.codesync.model.User;
import com.codesync.repository.UserRepository;
import com.codesync.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads "Authorization: Bearer <token>" and, if valid, populates the
 * SecurityContext with the matching User as the authenticated principal.
 * Requests without a valid token simply stay anonymous (handled per-route
 * in SecurityConfig), so this filter never itself rejects a request.
 */
@Component
public class TokenAuthFilter extends OncePerRequestFilter {

    private final SessionService sessionService;
    private final UserRepository userRepository;

    public TokenAuthFilter(SessionService sessionService, UserRepository userRepository) {
        this.sessionService = sessionService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            sessionService.getUserId(token)
                    .flatMap(userRepository::findById)
                    .ifPresent(user -> {
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }

        filterChain.doFilter(request, response);
    }
}
