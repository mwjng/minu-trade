package com.minupay.trade.broadcast.presentation;

import lombok.Getter;

import java.security.Principal;

@Getter
public class StompPrincipal implements Principal {

    private final Long userId;

    public StompPrincipal(Long userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
