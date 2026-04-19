package com.minupay.trade.paymentclient.dto;

import java.math.BigDecimal;

public record ChargeResponse(
        Long paymentId,
        Long walletId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String status
) {
}
