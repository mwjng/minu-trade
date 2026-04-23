package com.minupay.trade.paymentclient.dto;

import java.math.BigDecimal;

public record WalletTxRequest(
        BigDecimal amount,
        String reason,
        String idempotencyKey
) {
}
