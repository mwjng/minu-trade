package com.minupay.trade.order.application.dto;

import com.minupay.trade.order.domain.Execution;

import java.math.BigDecimal;
import java.time.Instant;

public record ExecutionInfo(
        Long id,
        Long buyOrderId,
        Long sellOrderId,
        String stockCode,
        BigDecimal price,
        int quantity,
        Instant executedAt
) {
    public static ExecutionInfo from(Execution execution) {
        return new ExecutionInfo(
                execution.getId(),
                execution.getBuyOrderId(),
                execution.getSellOrderId(),
                execution.getStockCode(),
                execution.getPrice().getAmount(),
                execution.getQuantity(),
                execution.getCreatedAt()
        );
    }
}
