package com.minupay.trade.stock.presentation.dto;

import com.minupay.trade.stock.application.dto.UpsertStockCommand;
import com.minupay.trade.stock.domain.Market;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record UpsertStockRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull Market market,
        String sector,
        @Positive int tickSize,
        Long marketCap,
        @NotNull LocalDate listedAt
) {
    public UpsertStockCommand toCommand() {
        return new UpsertStockCommand(code, name, market, sector, tickSize, marketCap, listedAt);
    }
}
