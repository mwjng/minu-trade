package com.minupay.trade.portfolio.presentation;

import com.minupay.trade.auth.infrastructure.LoginUser;
import com.minupay.trade.common.response.ApiResponse;
import com.minupay.trade.portfolio.application.PortfolioService;
import com.minupay.trade.portfolio.application.dto.PortfolioSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PortfolioSummary>> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getMyPortfolio(loginUser.getUserId())));
    }
}
