package com.minupay.trade.marketdata.infrastructure.persistence;

import com.minupay.trade.marketdata.domain.Quote;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuoteRepository extends MongoRepository<Quote, String> {
}
