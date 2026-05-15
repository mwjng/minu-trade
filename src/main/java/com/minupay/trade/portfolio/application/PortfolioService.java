package com.minupay.trade.portfolio.application;

import com.minupay.trade.common.money.Money;
import com.minupay.trade.holding.application.HoldingLookup;
import com.minupay.trade.holding.application.dto.HoldingInfo;
import com.minupay.trade.marketdata.application.QuoteLookup;
import com.minupay.trade.portfolio.application.dto.PortfolioItem;
import com.minupay.trade.portfolio.application.dto.PortfolioSummary;
import com.minupay.trade.portfolio.application.dto.Valuation;
import com.minupay.trade.stock.application.StockNameLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final HoldingLookup holdingLookup;
    private final QuoteLookup quoteLookup;
    private final StockNameLookup stockNameLookup;

    public PortfolioSummary getMyPortfolio(Long userId) {
        List<HoldingInfo> holdings = holdingLookup.findByUserId(userId);
        if (holdings.isEmpty()) {
            return new PortfolioSummary(
                    Money.ZERO, Money.ZERO, BigDecimal.ZERO,
                    Valuation.returnRate(BigDecimal.ZERO, BigDecimal.ZERO), List.of());
        }
        Map<String, String> names = stockNameLookup.findNamesByCodes(
                holdings.stream().map(HoldingInfo::stockCode).toList());

        List<PortfolioItem> items = holdings.stream()
                .map(h -> toItem(h, names.get(h.stockCode())))
                .toList();

        return summarize(items);
    }

    private PortfolioItem toItem(HoldingInfo holding, String stockName) {
        Money totalCost = Money.of(holding.avgPrice()).multiply(holding.quantity());
        Valuation valuation = quoteLookup.findCurrentPrice(holding.stockCode())
                .map(price -> Valuation.of(price, holding.quantity(), totalCost))
                .orElse(null);
        return new PortfolioItem(holding.stockCode(), stockName, holding.quantity(),
                holding.avgPrice(), totalCost, valuation);
    }

    private PortfolioSummary summarize(List<PortfolioItem> items) {
        Money totalCost = Money.ZERO;
        Money totalMarketValue = Money.ZERO;
        BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;
        for (PortfolioItem item : items) {
            totalCost = totalCost.add(item.totalCost());
            if (item.valuation() != null) {
                totalMarketValue = totalMarketValue.add(item.valuation().marketValue());
                totalUnrealizedPnl = totalUnrealizedPnl.add(item.valuation().unrealizedPnl());
            }
        }
        BigDecimal totalReturnRate = Valuation.returnRate(totalUnrealizedPnl, totalCost.getAmount());
        return new PortfolioSummary(totalCost, totalMarketValue, totalUnrealizedPnl, totalReturnRate, items);
    }
}
