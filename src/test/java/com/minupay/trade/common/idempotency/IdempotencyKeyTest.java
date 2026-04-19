package com.minupay.trade.common.idempotency;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyKeyTest {

    @Test
    void 시작시_IN_PROGRESS_상태() {
        IdempotencyKey key = IdempotencyKey.startInProgress("K1", "H1", Instant.now().plus(1, ChronoUnit.HOURS));
        assertThat(key.getStatus()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
        assertThat(key.getResponse()).isNull();
    }

    @Test
    void complete_후_응답이_저장되고_상태_COMPLETED() {
        IdempotencyKey key = IdempotencyKey.startInProgress("K1", "H1", Instant.now().plusSeconds(3600));
        key.complete("{\"orderId\":1}");
        assertThat(key.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(key.getResponse()).isEqualTo("{\"orderId\":1}");
    }

    @Test
    void 요청해시_매칭() {
        IdempotencyKey key = IdempotencyKey.startInProgress("K1", "H1", Instant.now().plusSeconds(3600));
        assertThat(key.matchesRequest("H1")).isTrue();
        assertThat(key.matchesRequest("H2")).isFalse();
    }

    @Test
    void 만료_판별() {
        Instant past = Instant.now().minusSeconds(1);
        IdempotencyKey key = IdempotencyKey.startInProgress("K1", "H1", past);
        assertThat(key.isExpired(Instant.now())).isTrue();
    }
}
