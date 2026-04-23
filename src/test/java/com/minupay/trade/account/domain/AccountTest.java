package com.minupay.trade.account.domain;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
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
        assertThat(account.getBalance()).isEqualByComparingTo("0");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0");
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

        account.deposit(new BigDecimal("100000"));

        assertThat(account.getBalance()).isEqualByComparingTo("100000");
        assertThat(account.availableBalance()).isEqualByComparingTo("100000");
    }

    @Test
    void withdraw_예수금_차감() {
        Account account = Account.create(1L);
        account.deposit(new BigDecimal("100000"));

        account.withdraw(new BigDecimal("30000"));

        assertThat(account.getBalance()).isEqualByComparingTo("70000");
    }

    @Test
    void withdraw_잔고보다_크면_예외() {
        Account account = Account.create(1L);
        account.deposit(new BigDecimal("100000"));

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("100001")))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INSUFFICIENT_BALANCE);
    }

    @Test
    void withdraw_예약된_금액은_출금불가() {
        Account account = Account.create(1L);
        account.deposit(new BigDecimal("100000"));
        account.reserve(new BigDecimal("70000"));

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("31000")))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INSUFFICIENT_BALANCE);
    }

    @Test
    void reserve_가용잔고에서_예약() {
        Account account = Account.create(1L);
        account.deposit(new BigDecimal("100000"));

        account.reserve(new BigDecimal("70000"));

        assertThat(account.getBalance()).isEqualByComparingTo("100000");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("70000");
        assertThat(account.availableBalance()).isEqualByComparingTo("30000");
    }

    @Test
    void reserve_가용잔고_부족시_예외() {
        Account account = Account.create(1L);
        account.deposit(new BigDecimal("100000"));
        account.reserve(new BigDecimal("60000"));

        assertThatThrownBy(() -> account.reserve(new BigDecimal("50000")))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INSUFFICIENT_BALANCE);
    }

    @Test
    void releaseReserve_예약_해제() {
        Account account = Account.create(1L);
        account.deposit(new BigDecimal("100000"));
        account.reserve(new BigDecimal("70000"));

        account.releaseReserve(new BigDecimal("20000"));

        assertThat(account.getReservedBalance()).isEqualByComparingTo("50000");
        assertThat(account.getBalance()).isEqualByComparingTo("100000");
        assertThat(account.availableBalance()).isEqualByComparingTo("50000");
    }

    @Test
    void releaseReserve_예약보다_크면_예외() {
        Account account = Account.create(1L);
        account.deposit(new BigDecimal("100000"));
        account.reserve(new BigDecimal("30000"));

        assertThatThrownBy(() -> account.releaseReserve(new BigDecimal("30001")))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INSUFFICIENT_RESERVED);
    }

    @Test
    void settleBuy_예약_및_잔고_동시_차감() {
        Account account = Account.create(1L);
        account.deposit(new BigDecimal("100000"));
        account.reserve(new BigDecimal("70000"));

        account.settleBuy(new BigDecimal("30000"));

        assertThat(account.getBalance()).isEqualByComparingTo("70000");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("40000");
        assertThat(account.availableBalance()).isEqualByComparingTo("30000");
    }

    @Test
    void settleBuy_예약보다_크면_예외() {
        Account account = Account.create(1L);
        account.deposit(new BigDecimal("100000"));
        account.reserve(new BigDecimal("30000"));

        assertThatThrownBy(() -> account.settleBuy(new BigDecimal("30001")))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INSUFFICIENT_RESERVED);
    }

    @Test
    void settleSell_대금_입금() {
        Account account = Account.create(1L);
        account.deposit(new BigDecimal("10000"));

        account.settleSell(new BigDecimal("70000"));

        assertThat(account.getBalance()).isEqualByComparingTo("80000");
        assertThat(account.availableBalance()).isEqualByComparingTo("80000");
    }

    @Test
    void 음수나_0_금액은_예외() {
        Account account = Account.create(1L);

        assertThatThrownBy(() -> account.deposit(BigDecimal.ZERO))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INVALID_AMOUNT);

        assertThatThrownBy(() -> account.reserve(new BigDecimal("-1")))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INVALID_AMOUNT);

        assertThatThrownBy(() -> account.settleSell(null))
                .isInstanceOf(MinuTradeException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_INVALID_AMOUNT);
    }
}
