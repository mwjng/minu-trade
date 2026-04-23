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
    ACCOUNT_INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY, "AC005", "Insufficient account balance"),
    ACCOUNT_INSUFFICIENT_RESERVED(HttpStatus.UNPROCESSABLE_ENTITY, "AC006", "Reserved balance is insufficient"),
    ACCOUNT_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "AC007", "Invalid account amount"),

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
    IDEMPOTENCY_IN_PROGRESS(HttpStatus.CONFLICT, "I002", "Idempotent request is still in progress"),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "O001", "Order not found"),
    ORDER_INVALID_PRICE(HttpStatus.BAD_REQUEST, "O002", "Invalid order price"),
    ORDER_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "O003", "Invalid order quantity"),
    ORDER_INVALID_STATE(HttpStatus.UNPROCESSABLE_ENTITY, "O004", "Invalid order state transition"),
    ORDER_OVERFILL(HttpStatus.UNPROCESSABLE_ENTITY, "O005", "Order fill exceeds quantity"),
    ORDER_TICK_NOT_ALIGNED(HttpStatus.BAD_REQUEST, "O006", "Price is not aligned with tick size"),
    ORDER_MARKET_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "O007", "Market order is not yet supported"),
    ORDER_FORBIDDEN(HttpStatus.FORBIDDEN, "O008", "Order does not belong to this account"),
    ORDER_CANCEL_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "O009", "Order cancel request timed out"),

    // Holding
    HOLDING_NOT_FOUND(HttpStatus.NOT_FOUND, "H001", "Holding not found"),
    HOLDING_INSUFFICIENT(HttpStatus.UNPROCESSABLE_ENTITY, "H002", "Holding quantity is insufficient"),
    HOLDING_INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "H003", "Invalid holding quantity"),
    HOLDING_INVALID_PRICE(HttpStatus.BAD_REQUEST, "H004", "Invalid holding price"),
    HOLDING_INSUFFICIENT_RESERVED(HttpStatus.UNPROCESSABLE_ENTITY, "H005", "Reserved holding quantity is insufficient");

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
