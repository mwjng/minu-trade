package com.minupay.trade.common.idempotency;

import com.minupay.trade.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "idempotency_keys")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey extends BaseTimeEntity {

    @Id
    @Column(name = "idem_key", length = 128)
    private String key;

    @Column(nullable = false, length = 64)
    private String requestHash;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String response;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotencyStatus status;

    @Column(nullable = false)
    private Instant expireAt;

    private IdempotencyKey(String key, String requestHash, IdempotencyStatus status, Instant expireAt) {
        this.key = key;
        this.requestHash = requestHash;
        this.status = status;
        this.expireAt = expireAt;
    }

    public static IdempotencyKey startInProgress(String key, String requestHash, Instant expireAt) {
        return new IdempotencyKey(key, requestHash, IdempotencyStatus.IN_PROGRESS, expireAt);
    }

    public void complete(String response) {
        this.response = response;
        this.status = IdempotencyStatus.COMPLETED;
    }

    public void fail() {
        this.status = IdempotencyStatus.FAILED;
    }

    public boolean matchesRequest(String requestHash) {
        return this.requestHash.equals(requestHash);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expireAt);
    }

    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getResponse() { return response; }
    public IdempotencyStatus getStatus() { return status; }
    public Instant getExpireAt() { return expireAt; }
}
