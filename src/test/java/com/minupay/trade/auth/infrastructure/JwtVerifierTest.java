package com.minupay.trade.auth.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtVerifierTest {

    private static final String SECRET = "minupay-dev-secret-key-must-be-at-least-32-characters";
    private static final String OTHER_SECRET = "completely-other-secret-minimum-32-characters-xx";

    private final JwtVerifier verifier = new JwtVerifier(SECRET);

    @Test
    void 같은_secret으로_발급된_토큰은_파싱된다() {
        String token = issue(SECRET, 1001L, "USER", 60_000);

        Optional<Claims> claims = verifier.parse(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("1001");
        assertThat(claims.get().get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void 다른_secret으로_발급된_토큰은_거부된다() {
        String token = issue(OTHER_SECRET, 1001L, "USER", 60_000);

        assertThat(verifier.parse(token)).isEmpty();
    }

    @Test
    void 만료된_토큰은_거부된다() {
        String token = issue(SECRET, 1001L, "USER", -1_000);

        assertThat(verifier.parse(token)).isEmpty();
    }

    @Test
    void 잘못된_형식의_토큰은_거부된다() {
        assertThat(verifier.parse("not-a-jwt")).isEmpty();
        assertThat(verifier.parse("")).isEmpty();
    }

    private static String issue(String secret, Long userId, String role, long ttlMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }
}
