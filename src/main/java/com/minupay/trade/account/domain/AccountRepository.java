package com.minupay.trade.account.domain;

import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);
    Optional<Account> findById(Long id);
    Optional<Account> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
