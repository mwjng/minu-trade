package com.minupay.trade.account.application;

/**
 * 다른 모듈이 Account 의 userId 를 해석할 때 사용하는 공개 API.
 */
public interface AccountLookup {

    Long getUserId(Long accountId);
}
