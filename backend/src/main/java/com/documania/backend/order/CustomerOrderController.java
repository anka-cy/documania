package com.documania.backend.order;

import com.documania.backend.order.dto.CreateOrderRequest;
import com.documania.backend.order.dto.OrderPageResponse;
import com.documania.backend.order.dto.OrderResponse;
import com.documania.backend.order.dto.RejectOrderRequest;
import com.documania.backend.order.dto.StaffCreateOrderRequest;
import com.documania.backend.order.dto.InvoiceResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
public class CustomerOrderController {
    private final CustomerOrderService orderService;

    public CustomerOrderController(CustomerOrderService orderService) { this.orderService = orderService; }

    @PostMapping("/client/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        return CustomerOrderMapper.toResponse(orderService.createForCurrentClient(authentication.getName(), request.offerId()));
    }

    @GetMapping("/client/orders")
    public List<OrderResponse> listMine(Authentication authentication) {
        return orderService.listForCurrentClient(authentication.getName()).stream()
            .map(CustomerOrderMapper::toResponse).toList();
    }

    @GetMapping("/client/orders/{id}/invoice")
    public InvoiceResponse invoice(@PathVariable java.util.UUID id, Authentication authentication) {
        return orderService.findInvoiceForCurrentClient(authentication.getName(), id);
    }

    @PatchMapping("/client/orders/{id}/cancel")
    public OrderResponse cancel(@PathVariable java.util.UUID id, Authentication authentication) {
        return CustomerOrderMapper.toResponse(orderService.cancelForCurrentClient(authentication.getName(), id));
    }

    @GetMapping("/staff/orders")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ORDER_READ')")
    public OrderPageResponse listAll(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) OrderStatus status
    ) {
        return orderService.listAll(query, status, page, size);
    }

    @PostMapping("/staff/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse createForClient(@Valid @RequestBody StaffCreateOrderRequest request,
                                         Authentication authentication) {
        CustomerOrderService.ConfirmedOrder result = orderService.createAndConfirmForClient(
            authentication.getName(), request.clientId(), request.offerId());
        return CustomerOrderMapper.toResponse(result.order());
    }

    @PatchMapping("/staff/orders/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse reject(@PathVariable java.util.UUID id,
                                @Valid @RequestBody RejectOrderRequest request,
                                Authentication authentication) {
        return CustomerOrderMapper.toResponse(orderService.reject(authentication.getName(), id, request.reason()));
    }

    @PatchMapping("/staff/orders/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse confirm(@PathVariable java.util.UUID id, Authentication authentication) {
        CustomerOrderService.ConfirmedOrder result = orderService.confirm(authentication.getName(), id);
        return CustomerOrderMapper.toResponse(result.order());
    }
}
