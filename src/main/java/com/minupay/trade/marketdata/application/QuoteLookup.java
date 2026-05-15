package com.minupay.trade.marketdata.application;

import java.util.Optional;

/**
 * 다른 모듈이 종목의 현재가를 조회할 때 사용하는 공개 API.
 * Redis 캐시 우선, 미스 시 MongoDB 로 폴백한다.
 */
public interface QuoteLookup {

    Optional<Long> findCurrentPrice(String stockCode);
}
