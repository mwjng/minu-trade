package com.minupay.trade.holding.domain;

import com.minupay.trade.common.entity.BaseTimeEntity;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.common.money.Money;
import com.minupay.trade.common.money.MoneyConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.RoundingMode;

@Entity
@Table(
        name = "holdings",
        uniqueConstraints = @UniqueConstraint(name = "uk_holdings_user_stock", columnNames = {"user_id", "stock_code"}),
        indexes = @Index(name = "idx_holdings_user", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding extends BaseTimeEntity {

    private static final int AVG_PRICE_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 12)
    private String stockCode;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, precision = 19, scale = 4)
    private Money avgPrice;

    @Version
    private Long version;

    private Holding(Long userId, String stockCode, int quantity, Money avgPrice) {
        this.userId = userId;
        this.stockCode = stockCode;
        this.quantity = quantity;
        this.reservedQuantity = 0;
        this.avgPrice = avgPrice;
    }

    public static Holding openForBuy(Long userId, String stockCode, int quantity, Money price) {
        ensurePositiveQuantity(quantity);
        ensurePositivePrice(price);
        return new Holding(userId, stockCode, quantity, price.setScale(AVG_PRICE_SCALE, RoundingMode.HALF_UP));
    }

    public int availableQuantity() {
        return quantity - reservedQuantity;
    }

    public void buy(int quantity, Money price) {
        ensurePositiveQuantity(quantity);
        ensurePositivePrice(price);
        Money existingCost = avgPrice.multiply(this.quantity);
        Money addedCost = price.multiply(quantity);
        int newQuantity = this.quantity + quantity;
        this.avgPrice = existingCost.add(addedCost)
                .divide(newQuantity, AVG_PRICE_SCALE, RoundingMode.HALF_UP);
        this.quantity = newQuantity;
    }

    public void reserve(int quantity) {
        ensurePositiveQuantity(quantity);
        if (availableQuantity() < quantity) {
            throw new MinuTradeException(ErrorCode.HOLDING_INSUFFICIENT);
        }
        this.reservedQuantity += quantity;
    }

    public void releaseReserve(int quantity) {
        ensurePositiveQuantity(quantity);
        if (reservedQuantity < quantity) {
            throw new MinuTradeException(ErrorCode.HOLDING_INSUFFICIENT_RESERVED);
        }
        this.reservedQuantity -= quantity;
    }

    public void settleSell(int quantity) {
        ensurePositiveQuantity(quantity);
        if (reservedQuantity < quantity) {
            throw new MinuTradeException(ErrorCode.HOLDING_INSUFFICIENT_RESERVED);
        }
        this.reservedQuantity -= quantity;
        this.quantity -= quantity;
    }

    public boolean isEmpty() {
        return quantity == 0 && reservedQuantity == 0;
    }

    private static void ensurePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new MinuTradeException(ErrorCode.HOLDING_INVALID_QUANTITY);
        }
    }

    private static void ensurePositivePrice(Money price) {
        if (price == null || !price.isGreaterThan(Money.ZERO)) {
            throw new MinuTradeException(ErrorCode.HOLDING_INVALID_PRICE);
        }
    }
}
