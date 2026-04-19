package com.minupay.trade.stock.application.dto;

import com.minupay.trade.stock.domain.Market;

import java.time.LocalDate;

public record UpsertStockCommand(
        String code,
        String name,
        Market market,
        String sector,
        int tickSize,
        Long marketCap,
        LocalDate listedAt
) {
}
