package com.minupay.trade.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "Invalid input value"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "Internal server error"),
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "C003", "Duplicate request"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "Unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "Forbidden"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "Invalid or expired token"),

    // Account
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "AC001", "Account not found"),
    ACCOUNT_ALREADY_EXISTS(HttpStatus.CONFLICT, "AC002", "Account already exists for user"),
    ACCOUNT_NOT_ACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "AC003", "Account is not active"),
    ACCOUNT_CLOSED(HttpStatus.UNPROCESSABLE_ENTITY, "AC004", "Account is closed"),

    // Stock
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "ST001", "Stock not found"),
    STOCK_DELISTED(HttpStatus.UNPROCESSABLE_ENTITY, "ST002", "Stock is delisted"),
    STOCK_NOT_TRADABLE(HttpStatus.UNPROCESSABLE_ENTITY, "ST003", "Stock is not tradable"),

    // Payment (minu-pay 연동)
    PAY_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "P001", "Pay service unavailable"),
    PAYMENT_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "P002", "Payment failed"),
    PAYMENT_INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY, "P003", "Insufficient wallet balance"),

    // Idempotency
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "I001", "Idempotency key reused with different request"),
    IDEMPOTENCY_IN_PROGRESS(HttpStatus.CONFLICT, "I002", "Idempotent request is still in progress");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
