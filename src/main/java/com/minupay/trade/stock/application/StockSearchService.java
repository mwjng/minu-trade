package com.minupay.trade.stock.application;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.minupay.trade.stock.application.dto.StockSearchResult;
import com.minupay.trade.stock.domain.StockStatus;
import com.minupay.trade.stock.infrastructure.search.ChosungConverter;
import com.minupay.trade.stock.infrastructure.search.StockSearchDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    private Query buildQuery(String keyword) {
        String chosung = ChosungConverter.toChosung(keyword);

        Query nameMatch = MatchQuery.of(m -> m.field("name").query(keyword))._toQuery();
        Query chosungMatch = MatchQuery.of(m -> m.field("nameChosung").query(chosung))._toQuery();
        Query statusFilter = TermQuery.of(t -> t
                .field("status")
                .value(FieldValue.of(StockStatus.TRADING.name())))._toQuery();

        return BoolQuery.of(b -> b
                .filter(statusFilter)
                .should(nameMatch)
                .should(chosungMatch)
                .minimumShouldMatch("1")
        )._toQuery();
    }

    private Pageable clampPageSize(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) return pageable;
        return Pageable.ofSize(MAX_PAGE_SIZE).withPage(pageable.getPageNumber());
    }
}
