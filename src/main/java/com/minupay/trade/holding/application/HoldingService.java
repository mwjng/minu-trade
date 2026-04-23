package com.minupay.trade.holding.application;

import com.minupay.trade.common.exception.ErrorCode;
import com.minupay.trade.common.exception.MinuTradeException;
import com.minupay.trade.holding.application.dto.HoldingInfo;
import com.minupay.trade.holding.domain.Holding;
import com.minupay.trade.holding.domain.HoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HoldingService {

    private final HoldingRepository holdingRepository;

    @Transactional
    public HoldingInfo applyBuy(Long userId, String stockCode, int quantity, BigDecimal price) {
        Optional<Holding> existing = holdingRepository.findByUserIdAndStockCode(userId, stockCode);
        if (existing.isPresent()) {
            Holding holding = existing.get();
            holding.buy(quantity, price);
            return HoldingInfo.from(holding);
        }
        Holding created = holdingRepository.save(Holding.openForBuy(userId, stockCode, quantity, price));
        return HoldingInfo.from(created);
    }

    @Transactional
    public HoldingInfo reserveSell(Long userId, String stockCode, int quantity) {
        Holding holding = loadHolding(userId, stockCode);
        holding.reserve(quantity);
        return HoldingInfo.from(holding);
    }

    @Transactional
    public HoldingInfo releaseSell(Long userId, String stockCode, int quantity) {
        Holding holding = loadHolding(userId, stockCode);
        holding.releaseReserve(quantity);
        return HoldingInfo.from(holding);
    }

    @Transactional
    public HoldingInfo settleSell(Long userId, String stockCode, int quantity) {
        Holding holding = loadHolding(userId, stockCode);
        holding.settleSell(quantity);
        HoldingInfo snapshot = HoldingInfo.from(holding);
        if (holding.isEmpty()) {
            holdingRepository.delete(holding);
        }
        return snapshot;
    }

    @Transactional(readOnly = true)
    public List<HoldingInfo> getMyHoldings(Long userId) {
        return holdingRepository.findAllByUserIdOrderByStockCodeAsc(userId).stream()
                .map(HoldingInfo::from)
                .toList();
    }

    private Holding loadHolding(Long userId, String stockCode) {
        return holdingRepository.findByUserIdAndStockCode(userId, stockCode)
                .orElseThrow(() -> new MinuTradeException(ErrorCode.HOLDING_NOT_FOUND));
    }
}
