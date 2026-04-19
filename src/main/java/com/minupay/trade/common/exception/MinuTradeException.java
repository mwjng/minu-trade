package com.minupay.trade.common.exception;

public class MinuTradeException extends RuntimeException {

    private final ErrorCode errorCode;

    public MinuTradeException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public MinuTradeException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
