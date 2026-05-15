package com.minupay.trade.holding.application;

import com.minupay.trade.holding.application.dto.HoldingInfo;

import java.util.List;

/**
 * 다른 모듈이 사용자의 보유 종목을 조회할 때 사용하는 공개 API.
 */
public interface HoldingLookup {

    List<HoldingInfo> findByUserId(Long userId);
}
