package com.minupay.trade.paymentclient;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.paymentclient.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayServiceRestClient implements PayServiceClient {

    private static final String RESILIENCE_INSTANCE = "payService";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final RestClient payServiceRestClient;

    @Override
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "chargeFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public ChargeResponse charge(ChargeRequest request) {
        return payServiceRestClient.post()
                .uri("/payments")
                .headers(h -> applyCommon(h, request.idempotencyKey()))
                .body(request)
                .retrieve()
                .body(ChargeResponse.class);
    }

    @Override
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "cancelFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public CancelResponse cancel(Long paymentId, CancelRequest request) {
        return payServiceRestClient.post()
                .uri("/payments/{id}/cancel", paymentId)
                .headers(h -> applyCommon(h, request.idempotencyKey()))
                .body(request)
                .retrieve()
                .body(CancelResponse.class);
    }

    @Override
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "partialCancelFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public CancelResponse partialCancel(Long paymentId, PartialCancelRequest request) {
        return payServiceRestClient.post()
                .uri("/payments/{id}/partial-cancel", paymentId)
                .headers(h -> applyCommon(h, request.idempotencyKey()))
                .body(request)
                .retrieve()
                .body(CancelResponse.class);
    }

    @Override
    @CircuitBreaker(name = RESILIENCE_INSTANCE, fallbackMethod = "creditWalletFallback")
    @Retry(name = RESILIENCE_INSTANCE)
    public WalletChargeResponse creditWallet(Long walletId, WalletChargeRequest request) {
        return payServiceRestClient.post()
                .uri("/wallets/{id}/charge", walletId)
                .headers(h -> applyCommon(h, request.idempotencyKey()))
                .body(request)
                .retrieve()
                .body(WalletChargeResponse.class);
    }

    private void applyCommon(HttpHeaders headers, String idempotencyKey) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            headers.set(IDEMPOTENCY_HEADER, idempotencyKey);
        }
    }

    @SuppressWarnings("unused")
    private ChargeResponse chargeFallback(ChargeRequest request, Throwable t) {
        throw mapFailure("charge", t);
    }

    @SuppressWarnings("unused")
    private CancelResponse cancelFallback(Long paymentId, CancelRequest request, Throwable t) {
        throw mapFailure("cancel", t);
    }

    @SuppressWarnings("unused")
    private CancelResponse partialCancelFallback(Long paymentId, PartialCancelRequest request, Throwable t) {
        throw mapFailure("partialCancel", t);
    }

    @SuppressWarnings("unused")
    private WalletChargeResponse creditWalletFallback(Long walletId, WalletChargeRequest request, Throwable t) {
        throw mapFailure("creditWallet", t);
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
