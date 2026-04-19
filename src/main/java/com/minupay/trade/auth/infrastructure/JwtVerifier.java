package com.minupay.trade.auth.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * minu-pay 가 발급한 JWT 를 검증한다. secret 은 minu-pay 와 공유한다.
 * trade 는 토큰을 발급하지 않는다 (검증 only).
 */
@Component
public class JwtVerifier {

    private final SecretKey secretKey;

    public JwtVerifier(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(
                    Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload()
            );
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
