package com.minupay.trade.paymentclient.dto;

import java.math.BigDecimal;

public record PartialCancelRequest(
        BigDecimal amount,
        String reason,
        String idempotencyKey
) {
}
