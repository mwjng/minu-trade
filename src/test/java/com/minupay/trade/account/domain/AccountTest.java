package com.minupay.trade.account.domain;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.common.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void 생성_시_ACTIVE_상태_잔고_0() {
        Account account = Account.create(1001L);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getUserId()).isEqualTo(1001L);
        assertThat(account.getBalance()).isEqualTo(Money.ZERO);
        assertThat(account.getReservedBalance()).isEqualTo(Money.ZERO);
    }

    @Test
    void ACTIVE가_아니면_주문_불가() {
        Account account = Account.of(1L, 1001L, AccountStatus.SUSPENDED);

        assertThatThrownBy(account::ensureCanPlaceOrder)
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    @Test
    void 정지_후_재활성화() {
        Account account = Account.create(1001L);
        account.suspend();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.SUSPENDED);

        account.reactivate();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void 해지된_계좌는_재활성화_불가() {
        Account account = Account.of(1L, 1001L, AccountStatus.CLOSED);

        assertThatThrownBy(account::reactivate)
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_CLOSED);
    }

    @Test
    void 해지된_계좌는_정지_불가() {
        Account account = Account.of(1L, 1001L, AccountStatus.CLOSED);

        assertThatThrownBy(account::suspend)
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_CLOSED);
    }

    @Test
    void deposit_예수금_증가() {
        Account account = Account.create(1L);

        account.deposit(Money.of(100000));

        assertThat(account.getBalance()).isEqualTo(Money.of(100000));
        assertThat(account.availableBalance()).isEqualTo(Money.of(100000));
    }

    @Test
    void withdraw_예수금_차감() {
        Account account = Account.create(1L);
        account.deposit(Money.of(100000));

        account.withdraw(Money.of(30000));

        assertThat(account.getBalance()).isEqualTo(Money.of(70000));
    }

    @Test
    void withdraw_잔고보다_크면_예외() {
        Account account = Account.create(1L);
        account.deposit(Money.of(100000));

        assertThatThrownBy(() -> account.withdraw(Money.of(100001)))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INSUFFICIENT_BALANCE);
    }

    @Test
    void withdraw_예약된_금액은_출금불가() {
        Account account = Account.create(1L);
        account.deposit(Money.of(100000));
        account.reserve(Money.of(70000));

        assertThatThrownBy(() -> account.withdraw(Money.of(31000)))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INSUFFICIENT_BALANCE);
    }

    @Test
    void reserve_가용잔고에서_예약() {
        Account account = Account.create(1L);
        account.deposit(Money.of(100000));

        account.reserve(Money.of(70000));

        assertThat(account.getBalance()).isEqualTo(Money.of(100000));
        assertThat(account.getReservedBalance()).isEqualTo(Money.of(70000));
        assertThat(account.availableBalance()).isEqualTo(Money.of(30000));
    }

    @Test
    void reserve_가용잔고_부족시_예외() {
        Account account = Account.create(1L);
        account.deposit(Money.of(100000));
        account.reserve(Money.of(60000));

        assertThatThrownBy(() -> account.reserve(Money.of(50000)))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INSUFFICIENT_BALANCE);
    }

    @Test
    void releaseReserve_예약_해제() {
        Account account = Account.create(1L);
        account.deposit(Money.of(100000));
        account.reserve(Money.of(70000));

        account.releaseReserve(Money.of(20000));

        assertThat(account.getReservedBalance()).isEqualTo(Money.of(50000));
        assertThat(account.getBalance()).isEqualTo(Money.of(100000));
        assertThat(account.availableBalance()).isEqualTo(Money.of(50000));
    }

    @Test
    void releaseReserve_예약보다_크면_예외() {
        Account account = Account.create(1L);
        account.deposit(Money.of(100000));
        account.reserve(Money.of(30000));

        assertThatThrownBy(() -> account.releaseReserve(Money.of(30001)))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INSUFFICIENT_RESERVED);
    }

    @Test
    void settleBuy_예약_및_잔고_동시_차감() {
        Account account = Account.create(1L);
        account.deposit(Money.of(100000));
        account.reserve(Money.of(70000));

        account.settleBuy(Money.of(30000));

        assertThat(account.getBalance()).isEqualTo(Money.of(70000));
        assertThat(account.getReservedBalance()).isEqualTo(Money.of(40000));
        assertThat(account.availableBalance()).isEqualTo(Money.of(30000));
    }

    @Test
    void settleBuy_예약보다_크면_예외() {
        Account account = Account.create(1L);
        account.deposit(Money.of(100000));
        account.reserve(Money.of(30000));

        assertThatThrownBy(() -> account.settleBuy(Money.of(30001)))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INSUFFICIENT_RESERVED);
    }

    @Test
    void settleSell_대금_입금() {
        Account account = Account.create(1L);
        account.deposit(Money.of(10000));

        account.settleSell(Money.of(70000));

        assertThat(account.getBalance()).isEqualTo(Money.of(80000));
        assertThat(account.availableBalance()).isEqualTo(Money.of(80000));
    }

    @Test
    void 음수나_0_금액은_예외() {
        Account account = Account.create(1L);

        assertThatThrownBy(() -> account.deposit(Money.ZERO))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INVALID_AMOUNT);

        assertThatThrownBy(() -> Money.of(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> account.settleSell(null))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INVALID_AMOUNT);
    }
}
