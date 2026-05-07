package com.minupay.trade.broadcast.application;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record StompFanoutMessage(
        String destination,
        Long userId,
        String payload
) {
    public static StompFanoutMessage publicMessage(String destination, String payload) {
        return new StompFanoutMessage(destination, null, payload);
    }

    public static StompFanoutMessage userMessage(Long userId, String destination, String payload) {
        return new StompFanoutMessage(destination, userId, payload);
    }

    @JsonIgnore
    public boolean isUserDestination() {
        return userId != null;
    }
}
