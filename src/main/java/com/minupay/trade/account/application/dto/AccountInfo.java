package com.minupay.trade.account.application.dto;

import com.minupay.trade.account.domain.Account;
import com.minupay.trade.account.domain.AccountStatus;

public record AccountInfo(
        Long id,
        Long userId,
        Long walletId,
        AccountStatus status
) {
    public static AccountInfo from(Account account) {
        return new AccountInfo(account.getId(), account.getUserId(), account.getWalletId(), account.getStatus());
    }
}
