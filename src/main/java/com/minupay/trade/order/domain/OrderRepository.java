package com.minupay.trade.order.domain;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByAccountIdOrderByIdDesc(Long accountId);

    @Query("select o from Order o where o.accountId = :accountId "
            + "and (:status is null or o.status = :status)")
    Page<Order> findByAccountId(@Param("accountId") Long accountId,
                                @Param("status") OrderStatus status,
                                Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    Optional<Order> findByIdForUpdate(Long id);
}
