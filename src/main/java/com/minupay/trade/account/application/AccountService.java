package com.minupay.trade.account.application;

import com.minupay.trade.account.application.dto.AccountForOrder;
import com.minupay.trade.account.application.dto.AccountInfo;
import com.minupay.trade.account.domain.Account;
import com.minupay.trade.account.domain.AccountRepository;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountInfo openAccount(Long userId) {
        if (accountRepository.existsByUserId(userId)) {
            throw new MinuTradeException(ErrorCode.ACCOUNT_ALREADY_EXISTS);
        }
        Account saved = accountRepository.save(Account.create(userId));
        return AccountInfo.from(saved);
    }

    @Transactional(readOnly = true)
    public AccountInfo getByUserId(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.ACCOUNT_NOT_FOUND));
        return AccountInfo.from(account);
    }

    @Transactional(readOnly = true)
    public AccountForOrder resolveForOrder(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.ensureCanPlaceOrder();
        return new AccountForOrder(account.getId());
    }
}
