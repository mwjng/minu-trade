package com.minupay.trade.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + amount);
        }
        this.amount = amount;
    }

    public static Money of(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money multiply(long multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier));
    }

    public Money divide(long divisor, int scale, RoundingMode roundingMode) {
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), scale, roundingMode));
    }

    public Money setScale(int scale, RoundingMode roundingMode) {
        return new Money(this.amount.setScale(scale, roundingMode));
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        return this.amount.compareTo(other.amount) < 0;
    }

    public long toLong() {
        return amount.longValueExact();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
