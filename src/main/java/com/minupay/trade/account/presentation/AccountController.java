package com.minupay.trade.account.presentation;

import com.minupay.trade.account.application.AccountService;
import com.minupay.trade.account.application.dto.AccountInfo;
import com.minupay.trade.account.presentation.dto.OpenAccountRequest;
import com.minupay.trade.auth.infrastructure.LoginUser;
import com.minupay.trade.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountInfo>> open(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody OpenAccountRequest request
    ) {
        AccountInfo info = accountService.openAccount(loginUser.getUserId(), request.walletId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(info));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AccountInfo>> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.getByUserId(loginUser.getUserId())));
    }
}
