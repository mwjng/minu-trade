package com.minupay.trade.account.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {
    Optional<AccountEntity> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
