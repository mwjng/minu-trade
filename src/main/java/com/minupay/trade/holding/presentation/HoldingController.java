package com.minupay.trade.holding.presentation;

import com.minupay.trade.auth.infrastructure.LoginUser;
import com.minupay.trade.common.response.ApiResponse;
import com.minupay.trade.holding.application.HoldingService;
import com.minupay.trade.holding.application.dto.HoldingInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/holdings")
@RequiredArgsConstructor
public class HoldingController {

    private final HoldingService holdingService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<HoldingInfo>>> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(ApiResponse.ok(holdingService.getMyHoldings(loginUser.getUserId())));
    }
}
