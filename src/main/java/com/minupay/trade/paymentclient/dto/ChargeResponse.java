package com.minupay.trade.paymentclient.dto;

import java.math.BigDecimal;

public record ChargeResponse(
        Long paymentId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String status
) {
}
