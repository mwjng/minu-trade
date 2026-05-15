package com.minupay.trade.stock.application;

import java.util.Collection;
import java.util.Map;

/**
 * 다른 모듈이 종목 코드 → 종목명 매핑이 필요할 때 사용하는 공개 API.
 */
public interface StockNameLookup {

    Map<String, String> findNamesByCodes(Collection<String> codes);
}
