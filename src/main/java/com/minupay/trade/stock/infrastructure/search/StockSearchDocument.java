package com.minupay.trade.stock.infrastructure.search;

import com.minupay.trade.stock.domain.Market;
import com.minupay.trade.stock.domain.StockStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;

@Document(indexName = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockSearchDocument {

    @Id
    private String code;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String nameChosung;

    @Field(type = FieldType.Keyword)
    private Market market;

    @Field(type = FieldType.Keyword)
    private String sector;

    @Field(type = FieldType.Integer)
    private int tickSize;

    @Field(type = FieldType.Long)
    private Long marketCap;

    @Field(type = FieldType.Keyword)
    private StockStatus status;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate listedAt;

    private StockSearchDocument(String code, String name, String nameChosung, Market market,
                                String sector, int tickSize, Long marketCap,
                                StockStatus status, LocalDate listedAt) {
        this.code = code;
        this.name = name;
        this.nameChosung = nameChosung;
        this.market = market;
        this.sector = sector;
        this.tickSize = tickSize;
        this.marketCap = marketCap;
        this.status = status;
        this.listedAt = listedAt;
    }

    public static StockSearchDocument of(String code, String name, Market market, String sector,
                                         int tickSize, Long marketCap,
                                         StockStatus status, LocalDate listedAt) {
        return new StockSearchDocument(
                code, name, ChosungConverter.toChosung(name),
                market, sector, tickSize, marketCap, status, listedAt
        );
    }
}
