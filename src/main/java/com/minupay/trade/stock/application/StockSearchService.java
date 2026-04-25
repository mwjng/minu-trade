package com.minupay.trade.stock.application;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.minupay.trade.stock.application.dto.StockSearchResult;
import com.minupay.trade.stock.application.dto.StockSuggestion;
import com.minupay.trade.stock.domain.StockStatus;
import com.minupay.trade.stock.infrastructure.search.ChosungConverter;
import com.minupay.trade.stock.infrastructure.search.StockSearchDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSearchService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SUGGEST_LIMIT = 10;
    private static final int DEFAULT_SUGGEST_LIMIT = 10;

    private final ElasticsearchOperations operations;

    public Page<StockSearchResult> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return Page.empty(pageable);
        }
        String trimmed = keyword.trim();
        Pageable safe = clampPageSize(pageable);

        NativeQuery query = NativeQuery.builder()
                .withQuery(buildQuery(trimmed))
                .withPageable(safe)
                .build();

        SearchHits<StockSearchDocument> hits = operations.search(query, StockSearchDocument.class);
        List<StockSearchResult> content = hits.stream()
                .map(hit -> StockSearchResult.from(hit.getContent()))
                .toList();
        return new PageImpl<>(content, safe, hits.getTotalHits());
    }

    public List<StockSuggestion> suggest(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        int safeLimit = clampSuggestLimit(limit);
        NativeQuery query = NativeQuery.builder()
                .withQuery(buildSuggestQuery(keyword.trim()))
                .withPageable(PageRequest.of(0, safeLimit))
                .build();

        SearchHits<StockSearchDocument> hits = operations.search(query, StockSearchDocument.class);
        return hits.stream()
                .map(hit -> StockSuggestion.from(hit.getContent()))
                .toList();
    }

    private Query buildQuery(String keyword) {
        String chosung = ChosungConverter.toChosung(keyword);

        Query nameMatch = MatchQuery.of(m -> m.field("name").query(keyword))._toQuery();
        Query chosungMatch = MatchQuery.of(m -> m.field("nameChosung").query(chosung))._toQuery();

        return BoolQuery.of(b -> b
                .filter(tradingStatusFilter())
                .should(nameMatch)
                .should(chosungMatch)
                .minimumShouldMatch("1")
        )._toQuery();
    }

    private Query buildSuggestQuery(String keyword) {
        String chosung = ChosungConverter.toChosung(keyword);
        Query namePrefix = prefixMatch(keyword, "name.suggest");
        Query chosungPrefix = prefixMatch(chosung, "nameChosung.suggest");

        return BoolQuery.of(b -> b
                .filter(tradingStatusFilter())
                .should(namePrefix)
                .should(chosungPrefix)
                .minimumShouldMatch("1")
        )._toQuery();
    }

    private Query prefixMatch(String query, String baseField) {
        return MultiMatchQuery.of(m -> m
                .query(query)
                .type(TextQueryType.BoolPrefix)
                .fields(baseField, baseField + "._2gram", baseField + "._3gram")
        )._toQuery();
    }

    private Query tradingStatusFilter() {
        return TermQuery.of(t -> t
                .field("status")
                .value(FieldValue.of(StockStatus.TRADING.name())))._toQuery();
    }

    private Pageable clampPageSize(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) return pageable;
        return Pageable.ofSize(MAX_PAGE_SIZE).withPage(pageable.getPageNumber());
    }

    private int clampSuggestLimit(int limit) {
        if (limit < 1) return DEFAULT_SUGGEST_LIMIT;
        return Math.min(limit, MAX_SUGGEST_LIMIT);
    }
}
