package com.minupay.trade.account.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record OpenAccountRequest(@NotNull Long walletId) {
}
