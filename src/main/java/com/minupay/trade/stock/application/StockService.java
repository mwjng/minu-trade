package com.minupay.trade.stock.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minupay.trade.common.config.KafkaConfig;
import com.minupay.trade.common.event.EventEnvelope;
import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.common.outbox.Outbox;
import com.minupay.trade.common.outbox.OutboxRepository;
import com.minupay.trade.common.trace.TraceIdFilter;
import com.minupay.trade.stock.application.dto.StockInfo;
import com.minupay.trade.stock.application.dto.UpsertStockCommand;
import com.minupay.trade.stock.domain.Stock;
import com.minupay.trade.stock.domain.StockRepository;
import com.minupay.trade.stock.domain.event.StockUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public StockInfo upsert(UpsertStockCommand cmd) {
        Stock stock = stockRepository.findById(cmd.code())
                .map(existing -> {
                    existing.updateProfile(cmd.name(), cmd.sector(), cmd.tickSize(), cmd.marketCap());
                    return existing;
                })
                .orElseGet(() -> Stock.register(
                        cmd.code(), cmd.name(), cmd.market(), cmd.sector(),
                        cmd.tickSize(), cmd.marketCap(), cmd.listedAt()
                ));
        Stock saved = stockRepository.save(stock);

        publishStockUpdated(saved);
        return StockInfo.from(saved);
    }

    @Transactional(readOnly = true)
    public StockInfo getByCode(String code) {
        Stock stock = stockRepository.findById(code)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.STOCK_NOT_FOUND));
        return StockInfo.from(stock);
    }

    private void publishStockUpdated(Stock stock) {
        StockUpdatedEvent event = StockUpdatedEvent.of(stock, MDC.get(TraceIdFilter.MDC_KEY));
        String payload;
        try {
            payload = EventEnvelope.from(event).toJson(objectMapper);
        } catch (JsonProcessingException e) {
            throw new MinuTradeException(ErrorCode.INTERNAL_ERROR);
        }
        outboxRepository.save(Outbox.create(
                stock.getCode(),
                StockUpdatedEvent.AGGREGATE_TYPE,
                StockUpdatedEvent.EVENT_TYPE,
                KafkaConfig.TOPIC_STOCK_UPDATED,
                stock.getCode(),
                payload
        ));
    }
}
