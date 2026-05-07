package com.minupay.trade.broadcast.presentation;

import com.minupay.trade.auth.infrastructure.JwtVerifier;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtChannelInterceptorTest {

    private static final String SECRET = "minupay-dev-secret-key-must-be-at-least-32-characters";

    private final JwtVerifier verifier = new JwtVerifier(SECRET);
    private final JwtChannelInterceptor interceptor = new JwtChannelInterceptor(verifier);

    @Test
    void CONNECT_이외_프레임은_그대로_통과() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, null);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isSameAs(message);
    }

    @Test
    void CONNECT_프레임_유효한_토큰이면_StompPrincipal_세팅() {
        String token = issue(1001L, 60_000);
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Bearer " + token);

        Message<?> result = interceptor.preSend(message, null);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        Principal user = accessor.getUser();
        assertThat(user).isInstanceOf(StompPrincipal.class);
        assertThat(((StompPrincipal) user).getUserId()).isEqualTo(1001L);
    }

    @Test
    void CONNECT_프레임_Authorization_헤더_없으면_거부() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void CONNECT_프레임_Bearer_접두사_없으면_거부() {
        String token = issue(1001L, 60_000);
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, token);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void CONNECT_프레임_만료된_토큰은_거부() {
        String token = issue(1001L, -1_000);
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Bearer " + token);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Message<byte[]> stompMessage(StompCommand command, String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (authHeader != null) {
            accessor.addNativeHeader("Authorization", authHeader);
        }
        return org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private String issue(Long userId, long ttlMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", "USER")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }
}
