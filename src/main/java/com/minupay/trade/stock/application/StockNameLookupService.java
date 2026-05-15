package com.minupay.trade.stock.application;

import com.minupay.trade.stock.domain.Stock;
import com.minupay.trade.stock.domain.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockNameLookupService implements StockNameLookup {

    private final StockRepository stockRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> findNamesByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Map.of();
        }
        return stockRepository.findAllById(codes).stream()
                .collect(Collectors.toMap(Stock::getCode, Stock::getName));
    }
}
