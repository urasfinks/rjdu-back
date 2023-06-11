package ru.jamsys;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import ru.jamsys.mistercraft.WebSocketHandler;

@Configuration
@EnableWebSocket
public class Config implements WebSocketConfigurer {
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(), "*");
    }
}