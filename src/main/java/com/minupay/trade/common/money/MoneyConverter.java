package com.minupay.trade.common.money;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter(autoApply = false)
public class MoneyConverter implements AttributeConverter<Money, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(Money money) {
        return money == null ? null : money.getAmount();
    }

    @Override
    public Money convertToEntityAttribute(BigDecimal amount) {
        return amount == null ? null : Money.of(amount);
    }
}
