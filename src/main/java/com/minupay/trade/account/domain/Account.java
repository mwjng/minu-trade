package com.minupay.trade.account.domain;

import com.minupay.trade.common.entity.BaseTimeEntity;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "accounts",
        indexes = @Index(name = "idx_user_id", columnList = "user_id", unique = true)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    private Account(Long userId, AccountStatus status) {
        this.userId = userId;
        this.status = status;
    }

    public static Account create(Long userId) {
        return new Account(userId, AccountStatus.ACTIVE);
    }

    /** 테스트 등에서 특정 상태의 Account 가 필요할 때 사용. */
    public static Account of(Long id, Long userId, AccountStatus status) {
        Account account = new Account(userId, status);
        account.id = id;
        return account;
    }

    public void ensureCanPlaceOrder() {
        if (status != AccountStatus.ACTIVE) {
            throw new MinuTradeException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    public void suspend() {
        if (status == AccountStatus.CLOSED) {
            throw new MinuTradeException(ErrorCode.ACCOUNT_CLOSED);
        }
        this.status = AccountStatus.SUSPENDED;
    }

    public void reactivate() {
        if (status == AccountStatus.CLOSED) {
            throw new MinuTradeException(ErrorCode.ACCOUNT_CLOSED);
        }
        this.status = AccountStatus.ACTIVE;
    }

    public void close() {
        this.status = AccountStatus.CLOSED;
    }
}
