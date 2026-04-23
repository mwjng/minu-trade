package com.minupay.trade.account.presentation;

import com.minupay.trade.account.application.AccountFacade;
import com.minupay.trade.account.application.AccountService;
import com.minupay.trade.account.application.dto.AccountInfo;
import com.minupay.trade.account.presentation.dto.DepositRequest;
import com.minupay.trade.account.presentation.dto.WithdrawRequest;
import com.minupay.trade.auth.infrastructure.LoginUser;
import com.minupay.trade.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountFacade accountFacade;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountInfo>> open(@AuthenticationPrincipal LoginUser loginUser) {
        AccountInfo info = accountService.openAccount(loginUser.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(info));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AccountInfo>> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.getByUserId(loginUser.getUserId())));
    }

    @PostMapping("/me/deposit")
    public ResponseEntity<ApiResponse<AccountInfo>> deposit(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody DepositRequest request
    ) {
        AccountInfo info = accountFacade.deposit(loginUser.getUserId(), request.toCommand());
        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<ApiResponse<AccountInfo>> withdraw(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody WithdrawRequest request
    ) {
        AccountInfo info = accountFacade.withdraw(loginUser.getUserId(), request.toCommand());
        return ResponseEntity.ok(ApiResponse.ok(info));
    }
}
