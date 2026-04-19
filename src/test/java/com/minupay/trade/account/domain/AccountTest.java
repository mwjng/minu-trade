package com.minupay.trade.account.domain;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void 생성_시_ACTIVE_상태() {
        Account account = Account.create(1001L, 55L);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getUserId()).isEqualTo(1001L);
        assertThat(account.getWalletId()).isEqualTo(55L);
    }

    @Test
    void ACTIVE가_아니면_주문_불가() {
        Account account = Account.of(1L, 1001L, 55L, AccountStatus.SUSPENDED);

        assertThatThrownBy(account::ensureCanPlaceOrder)
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    @Test
    void 정지_후_재활성화() {
        Account account = Account.create(1001L, 55L);
        account.suspend();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.SUSPENDED);

        account.reactivate();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void 해지된_계좌는_재활성화_불가() {
        Account account = Account.of(1L, 1001L, 55L, AccountStatus.CLOSED);

        assertThatThrownBy(account::reactivate)
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_CLOSED);
    }

    @Test
    void 해지된_계좌는_정지_불가() {
        Account account = Account.of(1L, 1001L, 55L, AccountStatus.CLOSED);

        assertThatThrownBy(account::suspend)
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_CLOSED);
    }
}
