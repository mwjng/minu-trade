package com.minupay.trade.account.domain;

import com.minupay.trade.common.entity.BaseTimeEntity;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedBalance;

    @Version
    private Long version;

    private Account(Long userId, AccountStatus status) {
        this.userId = userId;
        this.status = status;
        this.balance = BigDecimal.ZERO;
        this.reservedBalance = BigDecimal.ZERO;
    }

    public static Account create(Long userId) {
        return new Account(userId, AccountStatus.ACTIVE);
    }

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

    public BigDecimal availableBalance() {
        return balance.subtract(reservedBalance);
    }

    public void deposit(BigDecimal amount) {
        ensurePositive(amount);
        this.balance = this.balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        ensurePositive(amount);
        if (availableBalance().compareTo(amount) < 0) {
            throw new MinuTradeException(ErrorCode.ACCOUNT_INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void reserve(BigDecimal amount) {
        ensurePositive(amount);
        if (availableBalance().compareTo(amount) < 0) {
            throw new MinuTradeException(ErrorCode.ACCOUNT_INSUFFICIENT_BALANCE);
        }
        this.reservedBalance = this.reservedBalance.add(amount);
    }

    public void releaseReserve(BigDecimal amount) {
        ensurePositive(amount);
        if (reservedBalance.compareTo(amount) < 0) {
            throw new MinuTradeException(ErrorCode.ACCOUNT_INSUFFICIENT_RESERVED);
        }
        this.reservedBalance = this.reservedBalance.subtract(amount);
    }

    public void settleBuy(BigDecimal amount) {
        ensurePositive(amount);
        if (reservedBalance.compareTo(amount) < 0) {
            throw new MinuTradeException(ErrorCode.ACCOUNT_INSUFFICIENT_RESERVED);
        }
        this.reservedBalance = this.reservedBalance.subtract(amount);
        this.balance = this.balance.subtract(amount);
    }

    public void settleSell(BigDecimal amount) {
        ensurePositive(amount);
        this.balance = this.balance.add(amount);
    }

    private void ensurePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new MinuTradeException(ErrorCode.ACCOUNT_INVALID_AMOUNT);
        }
    }
}
