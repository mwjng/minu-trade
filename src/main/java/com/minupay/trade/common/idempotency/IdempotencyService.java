package com.minupay.trade.common.idempotency;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    public static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final IdempotencyKeyRepository repository;

    /**
     * IN_PROGRESS 로 INSERT. 이미 같은 key 가 있으면
     * {@link org.springframework.dao.DataIntegrityViolationException} 이 던져짐.
     * 호출자가 catch 해서 replay / 충돌 / 재시도 중 결정.
     */
    @Transactional
    public void acquireSlot(String key, String requestHash, Duration ttl) {
        repository.saveAndFlush(IdempotencyKey.startInProgress(
                key, requestHash, Instant.now().plus(ttl)));
    }

    @Transactional(readOnly = true)
    public IdempotencyKey getSlot(String key) {
        return repository.findById(key)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.INTERNAL_ERROR));
    }

    @Transactional
    public void complete(String key, String response) {
        IdempotencyKey slot = repository.findById(key)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.INTERNAL_ERROR));
        slot.complete(response);
    }

    @Transactional
    public void fail(String key) {
        repository.findById(key).ifPresent(IdempotencyKey::fail);
    }
}
