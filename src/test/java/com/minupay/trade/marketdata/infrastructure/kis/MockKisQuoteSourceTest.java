package com.minupay.trade.marketdata.infrastructure.kis;

import com.minupay.trade.marketdata.application.MarketDataIngestService;
import com.minupay.trade.marketdata.application.dto.QuoteSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MockKisQuoteSourceTest {

    @Mock MarketDataIngestService marketDataIngestService;

    MockKisQuoteSource source;

    @BeforeEach
    void setup() {
        source = new MockKisQuoteSource(marketDataIngestService);
        ReflectionTestUtils.setField(source, "stockCodes", List.of("005930", "000660"));
        ReflectionTestUtils.setField(source, "baseIntervalMs", 500L);
        ReflectionTestUtils.setField(source, "jitterRatio", 0.5);
    }

    @Test
    void 등록된_종목코드마다_시세를_생성하여_ingest_호출() {
        source.emit();

        ArgumentCaptor<QuoteSnapshot> captor = ArgumentCaptor.forClass(QuoteSnapshot.class);
        verify(marketDataIngestService, times(2)).ingest(captor.capture(), anyString());
        List<String> codes = captor.getAllValues().stream()
                .map(QuoteSnapshot::stockCode)
                .toList();
        assertThat(codes).containsExactly("005930", "000660");
    }

    @Test
    void 호가창은_매도_매수_각_10단계로_생성된다() {
        source.emit();

        ArgumentCaptor<QuoteSnapshot> captor = ArgumentCaptor.forClass(QuoteSnapshot.class);
        verify(marketDataIngestService, times(2)).ingest(captor.capture(), anyString());
        QuoteSnapshot first = captor.getAllValues().get(0);
        assertThat(first.askPrices()).hasSize(10);
        assertThat(first.bidPrices()).hasSize(10);
        assertThat(first.askPrices().get(0).price()).isGreaterThan(first.currentPrice());
        assertThat(first.bidPrices().get(0).price()).isLessThan(first.currentPrice());
    }
}
