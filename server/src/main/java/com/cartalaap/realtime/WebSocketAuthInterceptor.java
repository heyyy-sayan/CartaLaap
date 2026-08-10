package com.cartalaap.realtime;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    static final String PRINCIPAL_ATTRIBUTE = "cartalaapPrincipal";
    private final JwtDecoder jwtDecoder;

    public WebSocketAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String protocols = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (protocols == null) return unauthorized(response);
        String[] offered = protocols.split(",");
        boolean applicationProtocolOffered = false;
        for (String value : offered) {
            String protocol = value.trim();
            if ("cartalaap".equals(protocol)) {
                applicationProtocolOffered = true;
                continue;
            }
            try {
                Jwt jwt = jwtDecoder.decode(protocol);
                attributes.put(PRINCIPAL_ATTRIBUTE, jwt.getSubject());
            } catch (JwtException ignored) {
            }
        }
        return applicationProtocolOffered && attributes.containsKey(PRINCIPAL_ATTRIBUTE) || unauthorized(response);
    }

    private boolean unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
    }
}
