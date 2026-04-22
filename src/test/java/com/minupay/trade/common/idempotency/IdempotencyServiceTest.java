package com.minupay.trade.common.idempotency;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock IdempotencyKeyRepository repository;

    IdempotencyService service;

    @BeforeEach
    void setup() {
        service = new IdempotencyService(repository);
    }

    @Test
    void acquireSlot_성공() {
        when(repository.saveAndFlush(any(IdempotencyKey.class))).thenAnswer(inv -> inv.getArgument(0));

        service.acquireSlot("k-1", "hash-a", Duration.ofHours(1));

        ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(repository).saveAndFlush(captor.capture());
        IdempotencyKey saved = captor.getValue();
        assertThat(saved.getKey()).isEqualTo("k-1");
        assertThat(saved.getRequestHash()).isEqualTo("hash-a");
        assertThat(saved.getStatus()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
    }

    @Test
    void acquireSlot_중복이면_DataIntegrityViolationException_전파() {
        when(repository.saveAndFlush(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> service.acquireSlot("k-1", "hash-a", Duration.ofHours(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void complete_상태_COMPLETED_response_저장() {
        IdempotencyKey slot = IdempotencyKey.startInProgress("k-1", "hash-a",
                Instant.now().plus(Duration.ofHours(1)));
        when(repository.findById("k-1")).thenReturn(Optional.of(slot));

        service.complete("k-1", "{\"ok\":true}");

        assertThat(slot.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(slot.getResponse()).isEqualTo("{\"ok\":true}");
    }

    @Test
    void complete_슬롯_없으면_INTERNAL_ERROR() {
        when(repository.findById("k-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete("k-1", "..."))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    @Test
    void fail_슬롯있으면_FAILED_처리() {
        IdempotencyKey slot = IdempotencyKey.startInProgress("k-1", "hash-a",
                Instant.now().plus(Duration.ofHours(1)));
        when(repository.findById("k-1")).thenReturn(Optional.of(slot));

        service.fail("k-1");

        assertThat(slot.getStatus()).isEqualTo(IdempotencyStatus.FAILED);
    }

    @Test
    void fail_슬롯없어도_조용히_패스() {
        when(repository.findById("k-1")).thenReturn(Optional.empty());

        service.fail("k-1");

        verify(repository, never()).save(any());
    }
}
