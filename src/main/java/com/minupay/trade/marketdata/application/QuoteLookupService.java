package com.minupay.trade.marketdata.application;

import com.minupay.trade.marketdata.domain.Quote;
import com.minupay.trade.marketdata.infrastructure.cache.QuoteCacheRepository;
import com.minupay.trade.marketdata.infrastructure.persistence.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuoteLookupService implements QuoteLookup {

    private final QuoteCacheRepository quoteCacheRepository;
    private final QuoteRepository quoteRepository;

    @Override
    public Optional<Long> findCurrentPrice(String stockCode) {
        return quoteCacheRepository.findByStockCode(stockCode)
                .or(() -> quoteRepository.findById(stockCode))
                .map(Quote::currentPrice);
    }
}
