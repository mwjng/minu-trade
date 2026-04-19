package com.minupay.trade.account.domain;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;

public class Account {

    private Long id;
    private Long userId;
    private Long walletId;
    private AccountStatus status;

    private Account() {}

    public static Account create(Long userId, Long walletId) {
        Account account = new Account();
        account.userId = userId;
        account.walletId = walletId;
        account.status = AccountStatus.ACTIVE;
        return account;
    }

    public static Account of(Long id, Long userId, Long walletId, AccountStatus status) {
        Account account = new Account();
        account.id = id;
        account.userId = userId;
        account.walletId = walletId;
        account.status = status;
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

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getWalletId() { return walletId; }
    public AccountStatus getStatus() { return status; }
}
