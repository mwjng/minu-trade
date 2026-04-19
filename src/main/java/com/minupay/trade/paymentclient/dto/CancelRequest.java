package com.minupay.trade.paymentclient.dto;

public record CancelRequest(
        String reason,
        String idempotencyKey
) {
}
