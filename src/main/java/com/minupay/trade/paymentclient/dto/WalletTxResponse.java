package com.minupay.trade.paymentclient.dto;

import java.math.BigDecimal;

public record WalletTxResponse(
        Long walletId,
        BigDecimal amount,
        BigDecimal balanceAfter
) {
}
