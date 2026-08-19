package com.codesync.config;

import com.codesync.websocket.CodeSocketHandler;
import com.codesync.websocket.ScreenSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CodeSocketHandler codeSocketHandler;
    private final ScreenSocketHandler screenSocketHandler;

    @Value("${codesync.websocket.allowed-origins:*}")
    private String allowedOrigins;

    public WebSocketConfig(CodeSocketHandler codeSocketHandler, ScreenSocketHandler screenSocketHandler) {
        this.codeSocketHandler = codeSocketHandler;
        this.screenSocketHandler = screenSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(codeSocketHandler, "/code")
                .setAllowedOrigins(allowedOrigins.split(","));
        registry.addHandler(screenSocketHandler, "/screen")
                .setAllowedOrigins(allowedOrigins.split(","));
    }
}
