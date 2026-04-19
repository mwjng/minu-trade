package com.minupay.trade.stock.domain.event;

import com.minupay.trade.common.event.AbstractDomainEvent;
import com.minupay.trade.stock.domain.Market;
import com.minupay.trade.stock.domain.Stock;
import com.minupay.trade.stock.domain.StockStatus;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class StockUpdatedEvent extends AbstractDomainEvent {

    public static final String EVENT_TYPE = "StockUpdated";
    public static final String AGGREGATE_TYPE = "Stock";

    private final String code;
    private final String name;
    private final Market market;
    private final String sector;
    private final int tickSize;
    private final Long marketCap;
    private final StockStatus status;
    private final LocalDate listedAt;

    private StockUpdatedEvent(String traceId, Stock stock) {
        super(traceId);
        this.code = stock.getCode();
        this.name = stock.getName();
        this.market = stock.getMarket();
        this.sector = stock.getSector();
        this.tickSize = stock.getTickSize();
        this.marketCap = stock.getMarketCap();
        this.status = stock.getStatus();
        this.listedAt = stock.getListedAt();
    }

    public static StockUpdatedEvent of(Stock stock, String traceId) {
        return new StockUpdatedEvent(traceId, stock);
    }

    @Override public String getEventType()      { return EVENT_TYPE; }
    @Override public String getAggregateType()  { return AGGREGATE_TYPE; }
    @Override public String getAggregateId()    { return code; }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", code);
        payload.put("name", name);
        payload.put("market", market.name());
        payload.put("sector", sector);
        payload.put("tickSize", tickSize);
        payload.put("marketCap", marketCap);
        payload.put("status", status.name());
        payload.put("listedAt", listedAt.toString());
        return payload;
    }
}
