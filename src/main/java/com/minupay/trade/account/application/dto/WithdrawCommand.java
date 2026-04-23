package com.minupay.trade.account.application.dto;

import java.math.BigDecimal;

public record WithdrawCommand(
        BigDecimal amount,
        String idempotencyKey
) {
}
