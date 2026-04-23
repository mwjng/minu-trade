package com.minupay.trade.account.presentation.dto;

import com.minupay.trade.account.application.dto.DepositCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
        @NotBlank String idempotencyKey
) {
    public DepositCommand toCommand() {
        return new DepositCommand(amount, idempotencyKey);
    }
}
