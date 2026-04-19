package com.minupay.trade.stock.application;

import com.minupay.trade.stock.application.dto.StockSearchResult;
import com.minupay.trade.stock.domain.StockStatus;
import com.minupay.trade.stock.infrastructure.search.ChosungConverter;
import com.minupay.trade.stock.infrastructure.search.StockSearchDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSearchService {

    private static final int DEFAULT_SIZE = 20;

    private final ElasticsearchOperations operations;

    public List<StockSearchResult> search(String keyword, Integer size) {
        if (keyword == null || keyword.isBlank()) return List.of();
        String trimmed = keyword.trim();
        int limit = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, 100);

        Criteria nameCriteria = ChosungConverter.isChosungOnly(trimmed)
                ? new Criteria("nameChosung").matches(trimmed)
                : new Criteria("name").matches(trimmed);
        Criteria statusFilter = new Criteria("status").is(StockStatus.TRADING.name());

        CriteriaQuery query = new CriteriaQuery(nameCriteria.and(statusFilter))
                .setPageable(PageRequest.of(0, limit));

        SearchHits<StockSearchDocument> hits = operations.search(query, StockSearchDocument.class);
        return hits.stream()
                .map(hit -> StockSearchResult.from(hit.getContent()))
                .toList();
    }
}
