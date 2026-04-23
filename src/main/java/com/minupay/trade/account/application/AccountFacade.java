package com.minupay.trade.account.application;

import com.minupay.trade.account.application.dto.AccountInfo;
import com.minupay.trade.account.application.dto.DepositCommand;
import com.minupay.trade.account.application.dto.WithdrawCommand;
import com.minupay.trade.paymentclient.PayServiceClient;
import com.minupay.trade.paymentclient.dto.WalletTxRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountFacade {

    private static final String DEPOSIT_REASON = "TRADING_DEPOSIT";
    private static final String WITHDRAW_REASON = "TRADING_WITHDRAW";

    private final AccountService accountService;
    private final PayServiceClient payServiceClient;

    public AccountInfo deposit(Long userId, DepositCommand cmd) {
        payServiceClient.deduct(userId, new WalletTxRequest(cmd.amount(), DEPOSIT_REASON, cmd.idempotencyKey()));
        return accountService.applyDeposit(userId, cmd.amount());
    }

    public AccountInfo withdraw(Long userId, WithdrawCommand cmd) {
        AccountInfo afterDebit = accountService.applyWithdraw(userId, cmd.amount());
        try {
            payServiceClient.credit(userId, new WalletTxRequest(cmd.amount(), WITHDRAW_REASON, cmd.idempotencyKey()));
            return afterDebit;
        } catch (RuntimeException e) {
            compensateWithdraw(userId, cmd);
            throw e;
        }
    }

    private void compensateWithdraw(Long userId, WithdrawCommand cmd) {
        try {
            accountService.applyDeposit(userId, cmd.amount());
        } catch (RuntimeException compensationEx) {
            log.error("withdraw compensation failed userId={} amount={} idem={}",
                    userId, cmd.amount(), cmd.idempotencyKey(), compensationEx);
        }
    }
}
