package com.minupay.trade.account.infrastructure;

import com.minupay.trade.account.domain.Account;
import com.minupay.trade.account.domain.AccountStatus;
import com.minupay.trade.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "accounts",
        indexes = @Index(name = "idx_user_id", columnList = "user_id", unique = true)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    public static AccountEntity from(Account account) {
        AccountEntity entity = new AccountEntity();
        entity.id = account.getId();
        entity.userId = account.getUserId();
        entity.walletId = account.getWalletId();
        entity.status = account.getStatus();
        return entity;
    }

    public Account toDomain() {
        return Account.of(id, userId, walletId, status);
    }
}
