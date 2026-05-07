package com.minupay.trade.broadcast.application;

public interface StompFanoutPublisher {

    void publish(StompFanoutMessage message);
}
