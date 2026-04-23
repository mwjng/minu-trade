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

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountService implements AccountLookup {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public Long getUserId(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.ACCOUNT_NOT_FOUND))
                .getUserId();
    }

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

    @Transactional
    public AccountInfo releaseReserve(Long userId, BigDecimal amount) {
        Account account = loadForUpdate(userId);
        account.releaseReserve(amount);
        return AccountInfo.from(account);
    }

    @Transactional
    public AccountInfo settleBuy(Long userId, BigDecimal amount) {
        Account account = loadForUpdate(userId);
        account.settleBuy(amount);
        return AccountInfo.from(account);
    }

    @Transactional
    public AccountInfo settleSell(Long userId, BigDecimal amount) {
        Account account = loadForUpdate(userId);
        account.settleSell(amount);
        return AccountInfo.from(account);
    }

    private Account loadForUpdate(Long userId) {
        return accountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
