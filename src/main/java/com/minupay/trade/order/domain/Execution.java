package com.minupay.trade.order.domain;

import com.minupay.trade.common.entity.BaseTimeEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "executions",
        indexes = {
                @Index(name = "idx_exec_buy_order", columnList = "buy_order_id"),
                @Index(name = "idx_exec_sell_order", columnList = "sell_order_id"),
                @Index(name = "idx_exec_stock", columnList = "stock_code")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Execution extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long buyOrderId;

    @Column(nullable = false)
    private Long sellOrderId;

    @Column(nullable = false, length = 12)
    private String stockCode;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, precision = 19, scale = 4)
    private Money price;

    @Column(nullable = false)
    private int quantity;

    private Execution(Long buyOrderId, Long sellOrderId, String stockCode, Money price, int quantity) {
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.stockCode = stockCode;
        this.price = price;
        this.quantity = quantity;
    }

    public static Execution of(Long buyOrderId, Long sellOrderId, String stockCode, Money price, int quantity) {
        return new Execution(buyOrderId, sellOrderId, stockCode, price, quantity);
    }
}
