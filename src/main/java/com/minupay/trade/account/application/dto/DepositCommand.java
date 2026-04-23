package com.minupay.trade.account.application.dto;

import java.math.BigDecimal;

public record DepositCommand(
        BigDecimal amount,
        String idempotencyKey
) {
}
