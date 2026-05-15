package com.minupay.trade.portfolio.application;

import com.minupay.trade.common.money.Money;
import com.minupay.trade.holding.application.HoldingLookup;
import com.minupay.trade.holding.application.dto.HoldingInfo;
import com.minupay.trade.marketdata.application.QuoteLookup;
import com.minupay.trade.portfolio.application.dto.PortfolioItem;
import com.minupay.trade.portfolio.application.dto.PortfolioSummary;
import com.minupay.trade.stock.application.StockNameLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock HoldingLookup holdingLookup;
    @Mock QuoteLookup quoteLookup;
    @Mock StockNameLookup stockNameLookup;

    PortfolioService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioService(holdingLookup, quoteLookup, stockNameLookup);
    }

    @Test
    void 보유종목_없으면_빈_요약을_반환() {
        when(holdingLookup.findByUserId(1L)).thenReturn(List.of());

        PortfolioSummary summary = service.getMyPortfolio(1L);

        assertThat(summary.items()).isEmpty();
        assertThat(summary.totalCost()).isEqualTo(Money.ZERO);
        assertThat(summary.totalMarketValue()).isEqualTo(Money.ZERO);
        assertThat(summary.totalUnrealizedPnl()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.totalReturnRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 모든_종목에_시세가_있으면_평가지표_정상_계산() {
        when(holdingLookup.findByUserId(1L)).thenReturn(List.of(
                new HoldingInfo(1L, 1L, "005930", 10, 0, 10, new BigDecimal("70000.0000")),
                new HoldingInfo(2L, 1L, "000660", 5, 0, 5, new BigDecimal("100000.0000"))
        ));
        when(stockNameLookup.findNamesByCodes(any())).thenReturn(Map.of(
                "005930", "삼성전자", "000660", "SK하이닉스"
        ));
        when(quoteLookup.findCurrentPrice("005930")).thenReturn(Optional.of(80000L));
        when(quoteLookup.findCurrentPrice("000660")).thenReturn(Optional.of(90000L));

        PortfolioSummary summary = service.getMyPortfolio(1L);

        assertThat(summary.items()).hasSize(2);

        PortfolioItem samsung = summary.items().get(0);
        assertThat(samsung.stockCode()).isEqualTo("005930");
        assertThat(samsung.stockName()).isEqualTo("삼성전자");
        assertThat(samsung.totalCost()).isEqualTo(Money.of(700000L));
        assertThat(samsung.valuation().marketValue()).isEqualTo(Money.of(800000L));
        assertThat(samsung.valuation().unrealizedPnl()).isEqualByComparingTo("100000");
        assertThat(samsung.valuation().returnRate()).isEqualByComparingTo("0.1429");

        PortfolioItem hynix = summary.items().get(1);
        assertThat(hynix.totalCost()).isEqualTo(Money.of(500000L));
        assertThat(hynix.valuation().marketValue()).isEqualTo(Money.of(450000L));
        assertThat(hynix.valuation().unrealizedPnl()).isEqualByComparingTo("-50000");

        assertThat(summary.totalCost()).isEqualTo(Money.of(1200000L));
        assertThat(summary.totalMarketValue()).isEqualTo(Money.of(1250000L));
        assertThat(summary.totalUnrealizedPnl()).isEqualByComparingTo("50000");
    }

    @Test
    void 시세_누락된_종목은_valuation_null_total에서_제외() {
        when(holdingLookup.findByUserId(1L)).thenReturn(List.of(
                new HoldingInfo(1L, 1L, "005930", 10, 0, 10, new BigDecimal("70000")),
                new HoldingInfo(2L, 1L, "000660", 5, 0, 5, new BigDecimal("100000"))
        ));
        when(stockNameLookup.findNamesByCodes(any())).thenReturn(Map.of(
                "005930", "삼성전자", "000660", "SK하이닉스"
        ));
        when(quoteLookup.findCurrentPrice("005930")).thenReturn(Optional.of(80000L));
        when(quoteLookup.findCurrentPrice("000660")).thenReturn(Optional.empty());

        PortfolioSummary summary = service.getMyPortfolio(1L);

        PortfolioItem missing = summary.items().get(1);
        assertThat(missing.valuation()).isNull();

        assertThat(summary.totalMarketValue()).isEqualTo(Money.of(800000L));
        assertThat(summary.totalUnrealizedPnl()).isEqualByComparingTo("100000");
        assertThat(summary.totalCost()).isEqualTo(Money.of(1200000L));
    }

    @Test
    void 종목명_없으면_null_로_표시() {
        when(holdingLookup.findByUserId(1L)).thenReturn(List.of(
                new HoldingInfo(1L, 1L, "099999", 1, 0, 1, new BigDecimal("1000"))
        ));
        when(stockNameLookup.findNamesByCodes(any())).thenReturn(Map.of());
        when(quoteLookup.findCurrentPrice("099999")).thenReturn(Optional.of(1000L));

        PortfolioSummary summary = service.getMyPortfolio(1L);

        assertThat(summary.items().get(0).stockName()).isNull();
    }
}
