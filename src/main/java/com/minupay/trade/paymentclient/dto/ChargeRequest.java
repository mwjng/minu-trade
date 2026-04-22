package com.minupay.trade.paymentclient.dto;

import java.math.BigDecimal;

public record ChargeRequest(
        Long userId,
        BigDecimal amount,
        String reason,
        String idempotencyKey
) {
}
