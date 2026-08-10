package com.cartalaap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.cartalaap.realtime.RealtimeMessageGateway;
import com.cartalaap.realtime.WebSocketAuthInterceptor;
import com.cartalaap.realtime.WebSocketPrincipalHandshakeHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final RealtimeMessageGateway gateway;
    private final WebSocketAuthInterceptor authInterceptor;
    private final WebSocketPrincipalHandshakeHandler handshakeHandler;
    private final String[] allowedOrigins;

    public WebSocketConfig(RealtimeMessageGateway gateway, WebSocketAuthInterceptor authInterceptor,
            WebSocketPrincipalHandshakeHandler handshakeHandler,
            @Value("#{'${app.cors.allowed-origins}'.split(',')}") String[] allowedOrigins) {
        this.gateway = gateway;
        this.authInterceptor = authInterceptor;
        this.handshakeHandler = handshakeHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gateway, "/ws/messages")
                .addInterceptors(authInterceptor)
                .setHandshakeHandler(handshakeHandler)
                .setAllowedOrigins(allowedOrigins);
    }
}
