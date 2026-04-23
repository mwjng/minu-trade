package com.minupay.trade.account.presentation.dto;

import com.minupay.trade.account.application.dto.WithdrawCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        @NotBlank String idempotencyKey
) {
    public WithdrawCommand toCommand() {
        return new WithdrawCommand(amount, idempotencyKey);
    }
}
