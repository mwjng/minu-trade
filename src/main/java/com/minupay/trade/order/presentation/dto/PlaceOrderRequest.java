package com.minupay.trade.order.presentation.dto;

import com.minupay.trade.order.application.dto.PlaceOrderCommand;
import com.minupay.trade.order.domain.OrderSide;
import com.minupay.trade.order.domain.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record PlaceOrderRequest(
        @NotBlank String stockCode,
        @NotNull OrderSide side,
        @NotNull OrderType type,
        @PositiveOrZero BigDecimal price,
        @Positive int quantity,
        @NotBlank String idempotencyKey
) {
    public PlaceOrderCommand toCommand() {
        return new PlaceOrderCommand(stockCode, side, type, price, quantity, idempotencyKey);
    }
}
