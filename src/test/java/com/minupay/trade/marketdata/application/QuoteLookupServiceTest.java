package com.minupay.trade.marketdata.application;

import com.minupay.trade.marketdata.domain.Quote;
import com.minupay.trade.marketdata.infrastructure.cache.QuoteCacheRepository;
import com.minupay.trade.marketdata.infrastructure.persistence.QuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteLookupServiceTest {

    @Mock QuoteCacheRepository quoteCacheRepository;
    @Mock QuoteRepository quoteRepository;

    QuoteLookupService service;

    @BeforeEach
    void setUp() {
        service = new QuoteLookupService(quoteCacheRepository, quoteRepository);
    }

    @Test
    void Redis_히트면_Mongo는_조회하지_않는다() {
        when(quoteCacheRepository.findByStockCode("005930")).thenReturn(Optional.of(quote("005930", 80000L)));

        Optional<Long> price = service.findCurrentPrice("005930");

        assertThat(price).contains(80000L);
        verify(quoteRepository, never()).findById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void Redis_미스시_Mongo로_폴백() {
        when(quoteCacheRepository.findByStockCode("005930")).thenReturn(Optional.empty());
        when(quoteRepository.findById("005930")).thenReturn(Optional.of(quote("005930", 75000L)));

        Optional<Long> price = service.findCurrentPrice("005930");

        assertThat(price).contains(75000L);
    }

    @Test
    void 둘_다_없으면_empty() {
        when(quoteCacheRepository.findByStockCode("005930")).thenReturn(Optional.empty());
        when(quoteRepository.findById("005930")).thenReturn(Optional.empty());

        assertThat(service.findCurrentPrice("005930")).isEmpty();
    }

    private Quote quote(String code, long price) {
        return Quote.of(code, price, price, price, price, 0.0, 0L, List.of(), List.of(), Instant.now());
    }
}
