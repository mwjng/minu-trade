package com.minupay.trade.common.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsumedEventRecorder {

    private final ConsumedEventRepository repository;

    public boolean markIfAbsent(String eventId, String consumerGroup, String topic) {
        if (repository.existsByEventIdAndConsumerGroup(eventId, consumerGroup)) {
            return false;
        }
        try {
            repository.save(ConsumedEventEntity.mark(eventId, consumerGroup, topic));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
