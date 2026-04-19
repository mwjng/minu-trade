package com.minupay.trade.stock.domain;

import com.minupay.trade.common.entity.BaseTimeEntity;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "stocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseTimeEntity {

    @Id
    @Column(length = 12)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Market market;

    private String sector;

    @Column(nullable = false)
    private int tickSize;

    private Long marketCap;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockStatus status;

    @Column(nullable = false)
    private LocalDate listedAt;

    private Stock(String code, String name, Market market, String sector,
                  int tickSize, Long marketCap, StockStatus status, LocalDate listedAt) {
        this.code = code;
        this.name = name;
        this.market = market;
        this.sector = sector;
        this.tickSize = tickSize;
        this.marketCap = marketCap;
        this.status = status;
        this.listedAt = listedAt;
    }

    public static Stock register(String code, String name, Market market, String sector,
                                 int tickSize, Long marketCap, LocalDate listedAt) {
        validateTickSize(tickSize);
        return new Stock(code, name, market, sector, tickSize, marketCap, StockStatus.TRADING, listedAt);
    }

    public void updateProfile(String name, String sector, int tickSize, Long marketCap) {
        validateTickSize(tickSize);
        this.name = name;
        this.sector = sector;
        this.tickSize = tickSize;
        this.marketCap = marketCap;
    }

    public void halt() {
        if (status == StockStatus.DELISTED) {
            throw new MinuTradeException(ErrorCode.STOCK_DELISTED);
        }
        this.status = StockStatus.HALTED;
    }

    public void resume() {
        if (status == StockStatus.DELISTED) {
            throw new MinuTradeException(ErrorCode.STOCK_DELISTED);
        }
        this.status = StockStatus.TRADING;
    }

    public void delist() {
        this.status = StockStatus.DELISTED;
    }

    public boolean isTradable() {
        return status == StockStatus.TRADING;
    }

    private static void validateTickSize(int tickSize) {
        if (tickSize <= 0) {
            throw new MinuTradeException(ErrorCode.INVALID_INPUT);
        }
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public Market getMarket() { return market; }
    public String getSector() { return sector; }
    public int getTickSize() { return tickSize; }
    public Long getMarketCap() { return marketCap; }
    public StockStatus getStatus() { return status; }
    public LocalDate getListedAt() { return listedAt; }
}
