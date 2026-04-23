package com.minupay.trade.order.presentation;

import com.minupay.trade.auth.infrastructure.LoginUser;
import com.minupay.trade.common.response.ApiResponse;
import com.minupay.trade.order.application.OrderFacade;
import com.minupay.trade.order.application.dto.OrderInfo;
import com.minupay.trade.order.presentation.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderInfo>> place(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody PlaceOrderRequest request
    ) {
        OrderInfo info = orderFacade.placeOrder(loginUser.getUserId(), request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(info));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderInfo>> get(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderFacade.getForUser(loginUser.getUserId(), id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderInfo>> cancel(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderFacade.cancelOrder(loginUser.getUserId(), id)));
    }
}
