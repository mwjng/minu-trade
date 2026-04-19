package com.minupay.trade.order.domain;

import com.minupay.trade.common.entity.BaseTimeEntity;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_account", columnList = "account_id"),
                @Index(name = "idx_orders_stock_status", columnList = "stock_code, status"),
                @Index(name = "uk_orders_idem_key", columnList = "idempotency_key", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 12)
    private String stockCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;

    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int filledQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, length = 128)
    private String idempotencyKey;

    private Long paymentId;

    private Order(Long accountId, String stockCode, OrderSide side, OrderType type,
                  BigDecimal price, int quantity, String idempotencyKey) {
        this.accountId = accountId;
        this.stockCode = stockCode;
        this.side = side;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.filledQuantity = 0;
        this.status = OrderStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
    }

    public static Order place(Long accountId, String stockCode, OrderSide side, OrderType type,
                              BigDecimal price, int quantity, String idempotencyKey) {
        if (quantity <= 0) {
            throw new MinuTradeException(ErrorCode.ORDER_INVALID_QUANTITY);
        }
        if (type == OrderType.LIMIT) {
            if (price == null || price.signum() <= 0) {
                throw new MinuTradeException(ErrorCode.ORDER_INVALID_PRICE);
            }
        } else if (price != null) {
            throw new MinuTradeException(ErrorCode.ORDER_INVALID_PRICE);
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MinuTradeException(ErrorCode.INVALID_INPUT);
        }
        return new Order(accountId, stockCode, side, type, price, quantity, idempotencyKey);
    }

    public void accept(Long paymentId) {
        if (status != OrderStatus.PENDING) {
            throw new MinuTradeException(ErrorCode.ORDER_INVALID_STATE);
        }
        this.status = OrderStatus.ACCEPTED;
        this.paymentId = paymentId;
    }

    public void reject() {
        if (status != OrderStatus.PENDING) {
            throw new MinuTradeException(ErrorCode.ORDER_INVALID_STATE);
        }
        this.status = OrderStatus.REJECTED;
    }

    public void addFill(int qty) {
        if (qty <= 0) {
            throw new MinuTradeException(ErrorCode.ORDER_INVALID_QUANTITY);
        }
        if (status != OrderStatus.ACCEPTED && status != OrderStatus.PARTIALLY_FILLED) {
            throw new MinuTradeException(ErrorCode.ORDER_INVALID_STATE);
        }
        int newFilled = this.filledQuantity + qty;
        if (newFilled > quantity) {
            throw new MinuTradeException(ErrorCode.ORDER_OVERFILL);
        }
        this.filledQuantity = newFilled;
        this.status = (newFilled == quantity) ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
    }

    public void cancel() {
        if (status == OrderStatus.FILLED || status == OrderStatus.CANCELLED || status == OrderStatus.REJECTED) {
            throw new MinuTradeException(ErrorCode.ORDER_INVALID_STATE);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public BigDecimal totalAmount() {
        if (type != OrderType.LIMIT || price == null) return null;
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public int remainingQuantity() {
        return quantity - filledQuantity;
    }
}
