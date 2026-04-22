package com.minupay.trade.account.presentation;

import com.minupay.trade.account.application.AccountService;
import com.minupay.trade.account.application.dto.AccountInfo;
import com.minupay.trade.auth.infrastructure.LoginUser;
import com.minupay.trade.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountInfo>> open(@AuthenticationPrincipal LoginUser loginUser) {
        AccountInfo info = accountService.openAccount(loginUser.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(info));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AccountInfo>> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.getByUserId(loginUser.getUserId())));
    }
}
