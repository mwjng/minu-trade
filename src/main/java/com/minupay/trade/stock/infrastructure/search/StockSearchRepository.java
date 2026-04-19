package com.minupay.trade.stock.infrastructure.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface StockSearchRepository extends ElasticsearchRepository<StockSearchDocument, String> {
}
