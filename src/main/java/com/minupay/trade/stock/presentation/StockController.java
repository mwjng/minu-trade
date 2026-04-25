package com.minupay.trade.stock.presentation;

import com.minupay.trade.common.response.ApiResponse;
import com.minupay.trade.common.response.PageResponse;
import com.minupay.trade.stock.application.StockSearchService;
import com.minupay.trade.stock.application.StockService;
import com.minupay.trade.stock.application.dto.StockInfo;
import com.minupay.trade.stock.application.dto.StockSearchResult;
import com.minupay.trade.stock.application.dto.StockSuggestion;
import com.minupay.trade.stock.presentation.dto.UpsertStockRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockSearchService stockSearchService;

    @PostMapping
    public ResponseEntity<ApiResponse<StockInfo>> upsert(@Valid @RequestBody UpsertStockRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.upsert(request.toCommand())));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<StockInfo>> get(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.getByCode(code)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<StockSearchResult>>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<StockSearchResult> body = PageResponse.from(
                stockSearchService.search(keyword, pageable)
        );
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<StockSuggestion>>> suggest(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(stockSearchService.suggest(keyword, limit)));
    }
}
