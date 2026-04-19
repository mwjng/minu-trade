package com.minupay.trade.paymentclient.dto;

import java.math.BigDecimal;

public record WalletChargeRequest(
        BigDecimal amount,
        String reason,
        String idempotencyKey
) {
}
