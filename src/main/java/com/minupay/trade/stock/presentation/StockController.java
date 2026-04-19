package com.minupay.trade.stock.presentation;

import com.minupay.trade.common.response.ApiResponse;
import com.minupay.trade.stock.application.StockService;
import com.minupay.trade.stock.application.dto.StockInfo;
import com.minupay.trade.stock.presentation.dto.UpsertStockRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping
    public ResponseEntity<ApiResponse<StockInfo>> upsert(@Valid @RequestBody UpsertStockRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.upsert(request.toCommand())));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<StockInfo>> get(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.getByCode(code)));
    }
}
