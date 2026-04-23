package com.minupay.trade.paymentclient;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.paymentclient.dto.WalletTxRequest;
import com.minupay.trade.paymentclient.dto.WalletTxResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayServiceRestClient implements PayServiceClient {

    private static final String RESILIENCE_INSTANCE = "payService";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient payServiceRestClient;

    @Value("${pay-service.internal-api-key:}")
    private String internalApiKey;

    @Override
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "deductFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public WalletTxResponse deduct(Long userId, WalletTxRequest request) {
        return payServiceRestClient.post()
                .uri("/api/internal/wallets/{userId}/deduct", userId)
                .headers(h -> applyCommon(h, request.idempotencyKey()))
                .body(request)
                .retrieve()
                .body(WalletTxResponse.class);
    }

    @Override
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "creditFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public WalletTxResponse credit(Long userId, WalletTxRequest request) {
        return payServiceRestClient.post()
                .uri("/api/internal/wallets/{userId}/credit", userId)
                .headers(h -> applyCommon(h, request.idempotencyKey()))
                .body(request)
                .retrieve()
                .body(WalletTxResponse.class);
    }

    private void applyCommon(HttpHeaders headers, String idempotencyKey) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            headers.set(IDEMPOTENCY_HEADER, idempotencyKey);
        }
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set(INTERNAL_API_KEY_HEADER, internalApiKey);
        }
    }

    @SuppressWarnings("unused")
    private WalletTxResponse deductFallback(Long userId, WalletTxRequest request, Throwable t) {
        throw mapFailure("deduct", t);
    }

    @SuppressWarnings("unused")
    private WalletTxResponse creditFallback(Long userId, WalletTxRequest request, Throwable t) {
        throw mapFailure("credit", t);
    }

    private MinuTradeException mapFailure(String operation, Throwable t) {
        log.warn("pay-service {} failed: {}", operation, t.toString());
        if (t instanceof HttpClientErrorException clientErr) {
            if (clientErr.getStatusCode().value() == 409) {
                return new MinuTradeException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }
            if (clientErr.getStatusCode().value() == 422) {
                return new MinuTradeException(ErrorCode.PAYMENT_INSUFFICIENT_BALANCE);
            }
            return new MinuTradeException(ErrorCode.PAYMENT_FAILED);
        }
        if (t instanceof HttpServerErrorException || t instanceof ResourceAccessException) {
            return new MinuTradeException(ErrorCode.PAY_SERVICE_UNAVAILABLE);
        }
        return new MinuTradeException(ErrorCode.PAY_SERVICE_UNAVAILABLE);
    }
}
