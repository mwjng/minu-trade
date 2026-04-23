package com.minupay.trade.account.application.dto;

import com.minupay.trade.account.domain.Account;
import com.minupay.trade.account.domain.AccountStatus;

import java.math.BigDecimal;

public record AccountInfo(
        Long id,
        Long userId,
        AccountStatus status,
        BigDecimal balance,
        BigDecimal reservedBalance,
        BigDecimal availableBalance
) {
    public static AccountInfo from(Account account) {
        return new AccountInfo(
                account.getId(),
                account.getUserId(),
                account.getStatus(),
                account.getBalance(),
                account.getReservedBalance(),
                account.availableBalance()
        );
    }
}
