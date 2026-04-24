package com.minupay.trade.stock.application;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.minupay.trade.stock.application.dto.StockSearchResult;
import com.minupay.trade.stock.domain.Market;
import com.minupay.trade.stock.domain.StockStatus;
import com.minupay.trade.stock.infrastructure.search.StockSearchDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockSearchServiceTest {

    @Mock ElasticsearchOperations operations;
    StockSearchService service;

    @BeforeEach
    void setup() {
        service = new StockSearchService(operations);
    }

    @SuppressWarnings("unchecked")
    private SearchHits<StockSearchDocument> hitsOf(List<StockSearchDocument> docs, long total) {
        SearchHits<StockSearchDocument> hits = mock(SearchHits.class);
        List<SearchHit<StockSearchDocument>> hitList = docs.stream()
                .map(doc -> {
                    SearchHit<StockSearchDocument> h = mock(SearchHit.class);
                    given(h.getContent()).willReturn(doc);
                    return h;
                })
                .toList();
        given(hits.stream()).willReturn(hitList.stream());
        given(hits.getTotalHits()).willReturn(total);
        return hits;
    }

    private StockSearchDocument sample(String code, String name) {
        return StockSearchDocument.of(code, name, Market.KOSPI, "반도체",
                100, 500_000_000_000L, StockStatus.TRADING, LocalDate.of(1975, 6, 11));
    }

    private NativeQuery captureQuery() {
        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(operations).search(captor.capture(), eq(StockSearchDocument.class));
        return captor.getValue();
    }

    @Test
    void 키워드가_비어있거나_null이면_ES_호출없이_빈_페이지() {
        assertThat(service.search("  ", PageRequest.of(0, 20)).getContent()).isEmpty();
        assertThat(service.search(null, PageRequest.of(0, 20)).getContent()).isEmpty();
        verifyNoInteractions(operations);
    }

    @Test
    void 일반_키워드는_name_과_nameChosung_을_should로_OR_검색() {
        SearchHits<StockSearchDocument> hits = hitsOf(List.of(sample("005930", "삼성전자")), 1L);
        given(operations.search(any(NativeQuery.class), eq(StockSearchDocument.class))).willReturn(hits);

        Page<StockSearchResult> page = service.search("삼성", PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        BoolQuery bool = captureQuery().getQuery().bool();
        assertThat(bool.filter()).hasSize(1);
        assertThat(bool.should()).hasSize(2);

        List<String> shouldFields = bool.should().stream()
                .map(q -> q.match().field())
                .toList();
        assertThat(shouldFields).containsExactlyInAnyOrder("name", "nameChosung");
    }

    @Test
    void 자소음절_혼합_입력은_초성변환된_값을_nameChosung_에_질의() {
        SearchHits<StockSearchDocument> hits = hitsOf(List.of(), 0L);
        given(operations.search(any(NativeQuery.class), eq(StockSearchDocument.class))).willReturn(hits);

        service.search("삼ㅅ", PageRequest.of(0, 20));

        BoolQuery bool = captureQuery().getQuery().bool();
        Query chosungMatch = bool.should().stream()
                .filter(q -> "nameChosung".equals(q.match().field()))
                .findFirst().orElseThrow();
        assertThat(chosungMatch.match().query().stringValue()).isEqualTo("ㅅㅅ");

        Query nameMatch = bool.should().stream()
                .filter(q -> "name".equals(q.match().field()))
                .findFirst().orElseThrow();
        assertThat(nameMatch.match().query().stringValue()).isEqualTo("삼ㅅ");
    }

    @Test
    void 순수_초성_입력도_nameChosung에_원본_그대로_질의() {
        SearchHits<StockSearchDocument> hits = hitsOf(List.of(), 0L);
        given(operations.search(any(NativeQuery.class), eq(StockSearchDocument.class))).willReturn(hits);

        service.search("ㅅㅅ", PageRequest.of(0, 20));

        BoolQuery bool = captureQuery().getQuery().bool();
        Query chosungMatch = bool.should().stream()
                .filter(q -> "nameChosung".equals(q.match().field()))
                .findFirst().orElseThrow();
        assertThat(chosungMatch.match().query().stringValue()).isEqualTo("ㅅㅅ");
    }

    @Test
    void TRADING_상태_필터가_filter절에_포함() {
        SearchHits<StockSearchDocument> hits = hitsOf(List.of(), 0L);
        given(operations.search(any(NativeQuery.class), eq(StockSearchDocument.class))).willReturn(hits);

        service.search("삼성", PageRequest.of(0, 20));

        BoolQuery bool = captureQuery().getQuery().bool();
        Query filter = bool.filter().get(0);
        assertThat(filter.term().field()).isEqualTo("status");
        assertThat(filter.term().value().stringValue()).isEqualTo(StockStatus.TRADING.name());
    }

    @Test
    void 페이지_번호와_사이즈가_ES_쿼리에_전달된다() {
        SearchHits<StockSearchDocument> hits = hitsOf(List.of(), 0L);
        given(operations.search(any(NativeQuery.class), eq(StockSearchDocument.class))).willReturn(hits);

        service.search("삼성", PageRequest.of(2, 30));

        Pageable pageable = captureQuery().getPageable();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(30);
    }

    @Test
    void 페이지_사이즈는_100을_초과하지_못한다() {
        SearchHits<StockSearchDocument> hits = hitsOf(List.of(), 0L);
        given(operations.search(any(NativeQuery.class), eq(StockSearchDocument.class))).willReturn(hits);

        service.search("삼성", PageRequest.of(0, 500));

        assertThat(captureQuery().getPageable().getPageSize()).isEqualTo(100);
    }

    @Test
    void 총_hit_수를_페이지에_반영() {
        SearchHits<StockSearchDocument> hits = hitsOf(List.of(sample("005930", "삼성전자")), 42L);
        given(operations.search(any(NativeQuery.class), eq(StockSearchDocument.class))).willReturn(hits);

        Page<StockSearchResult> page = service.search("삼성", PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(42L);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }
}
