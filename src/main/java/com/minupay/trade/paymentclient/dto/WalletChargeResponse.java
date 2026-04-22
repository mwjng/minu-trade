package com.minupay.trade.paymentclient.dto;

import java.math.BigDecimal;

public record WalletChargeResponse(
        BigDecimal amount,
        BigDecimal balanceAfter
) {
}
