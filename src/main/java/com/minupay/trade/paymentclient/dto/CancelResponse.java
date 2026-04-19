package com.minupay.trade.paymentclient.dto;

import java.math.BigDecimal;

public record CancelResponse(
        Long paymentId,
        BigDecimal refundedAmount,
        BigDecimal balanceAfter,
        String status
) {
}
