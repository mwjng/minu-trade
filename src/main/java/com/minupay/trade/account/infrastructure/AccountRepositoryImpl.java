package com.minupay.trade.account.infrastructure;

import com.minupay.trade.account.domain.Account;
import com.minupay.trade.account.domain.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    @Override
    public Account save(Account account) {
        return jpaRepository.save(AccountEntity.from(account)).toDomain();
    }

    @Override
    public Optional<Account> findById(Long id) {
        return jpaRepository.findById(id).map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).map(AccountEntity::toDomain);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpaRepository.existsByUserId(userId);
    }
}
