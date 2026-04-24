package com.minupay.trade.order.presentation;

import com.minupay.trade.auth.infrastructure.LoginUser;
import com.minupay.trade.common.response.ApiResponse;
import com.minupay.trade.common.response.PageResponse;
import com.minupay.trade.order.application.OrderFacade;
import com.minupay.trade.order.application.dto.ExecutionInfo;
import com.minupay.trade.order.application.dto.OrderInfo;
import com.minupay.trade.order.domain.OrderStatus;
import com.minupay.trade.order.presentation.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderInfo>>> list(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<OrderInfo> body = PageResponse.from(
                orderFacade.listForUser(loginUser.getUserId(), status, pageable)
        );
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderInfo>> get(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderFacade.getForUser(loginUser.getUserId(), id)));
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<ApiResponse<PageResponse<ExecutionInfo>>> executions(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PageResponse<ExecutionInfo> body = PageResponse.from(
                orderFacade.listExecutionsForUser(loginUser.getUserId(), id, pageable)
        );
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderInfo>> cancel(
            @AuthenticationPrincipal LoginUser loginUser,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderFacade.cancelOrder(loginUser.getUserId(), id)));
    }
}
