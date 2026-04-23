package com.minupay.trade.account.application;

import com.minupay.trade.account.application.dto.AccountInfo;
import com.minupay.trade.account.application.dto.DepositCommand;
import com.minupay.trade.account.application.dto.WithdrawCommand;
import com.minupay.trade.paymentclient.PayServiceClient;
import com.minupay.trade.paymentclient.dto.WalletTxRequest;
import com.minupay.trade.paymentclient.dto.WalletTxResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountFacadeTest {

    @Mock AccountService accountService;
    @Mock PayServiceClient payServiceClient;

    AccountFacade facade;

    @BeforeEach
    void setup() {
        facade = new AccountFacade(accountService, payServiceClient);
    }

    @Test
    void 입금_요청_시_pay_deduct_후_applyDeposit_호출() {
        BigDecimal amount = new BigDecimal("50000");
        DepositCommand cmd = new DepositCommand(amount, "idem-1");
        AccountInfo expected = mock(AccountInfo.class);
        when(accountService.applyDeposit(eq(1L), eq(amount))).thenReturn(expected);

        AccountInfo result = facade.deposit(1L, cmd);

        WalletTxRequest req = new WalletTxRequest(amount, "TRADING_DEPOSIT", "idem-1");
        verify(payServiceClient).deduct(1L, req);
        verify(accountService).applyDeposit(1L, amount);
        org.assertj.core.api.Assertions.assertThat(result).isSameAs(expected);
    }

    @Test
    void 입금_시_pay_deduct_실패하면_DB_미변경() {
        DepositCommand cmd = new DepositCommand(new BigDecimal("50000"), "idem-2");
        when(payServiceClient.deduct(anyLong(), any())).thenThrow(new RuntimeException("pay down"));

        assertThatThrownBy(() -> facade.deposit(1L, cmd))
                .isInstanceOf(RuntimeException.class);

        verify(accountService, never()).applyDeposit(anyLong(), any());
    }

    @Test
    void 출금_요청_시_applyWithdraw_후_pay_credit_호출() {
        BigDecimal amount = new BigDecimal("30000");
        WithdrawCommand cmd = new WithdrawCommand(amount, "idem-3");
        AccountInfo afterDebit = mock(AccountInfo.class);
        when(accountService.applyWithdraw(eq(1L), eq(amount))).thenReturn(afterDebit);
        when(payServiceClient.credit(anyLong(), any())).thenReturn(new WalletTxResponse(10L, amount, BigDecimal.ZERO));

        AccountInfo result = facade.withdraw(1L, cmd);

        verify(accountService).applyWithdraw(1L, amount);
        WalletTxRequest req = new WalletTxRequest(amount, "TRADING_WITHDRAW", "idem-3");
        verify(payServiceClient).credit(1L, req);
        org.assertj.core.api.Assertions.assertThat(result).isSameAs(afterDebit);
    }

    @Test
    void 출금_중_pay_credit_실패하면_applyDeposit으로_보상() {
        BigDecimal amount = new BigDecimal("30000");
        WithdrawCommand cmd = new WithdrawCommand(amount, "idem-4");
        AccountInfo afterDebit = mock(AccountInfo.class);
        when(accountService.applyWithdraw(eq(1L), eq(amount))).thenReturn(afterDebit);
        when(payServiceClient.credit(anyLong(), any())).thenThrow(new RuntimeException("pay down"));

        assertThatThrownBy(() -> facade.withdraw(1L, cmd))
                .isInstanceOf(RuntimeException.class);

        verify(accountService).applyWithdraw(1L, amount);
        verify(accountService).applyDeposit(1L, amount);
    }

    @Test
    void 출금_보상_실패해도_원래_예외가_전파된다() {
        BigDecimal amount = new BigDecimal("30000");
        WithdrawCommand cmd = new WithdrawCommand(amount, "idem-5");
        when(accountService.applyWithdraw(eq(1L), eq(amount))).thenReturn(mock(AccountInfo.class));
        when(payServiceClient.credit(anyLong(), any())).thenThrow(new RuntimeException("pay down"));
        when(accountService.applyDeposit(anyLong(), any())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> facade.withdraw(1L, cmd))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("pay down");
    }
}
