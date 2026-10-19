package dev.piercerobinson.stockroom.order;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.piercerobinson.stockroom.common.PageResponse;
import dev.piercerobinson.stockroom.order.OrderDtos.CreateOrderRequest;
import dev.piercerobinson.stockroom.order.OrderDtos.OrderResponse;
import dev.piercerobinson.stockroom.order.OrderDtos.OrderSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	@Operation(summary = "List orders, optionally filtered by status")
	public PageResponse<OrderSummary> list(@RequestParam(required = false) OrderStatus status,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return orderService.list(status, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get an order with its lines")
	public OrderResponse get(@PathVariable Long id) {
		return orderService.get(id);
	}

	@PostMapping
	@Operation(summary = "Create a draft order")
	public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
			Authentication authentication) {
		OrderResponse created = orderService.create(request, authentication.getName());
		return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
	}

	@PostMapping("/{id}/confirm")
	@Operation(summary = "Confirm an order and take its stock (all lines or nothing)")
	public OrderResponse confirm(@PathVariable Long id, Authentication authentication) {
		return orderService.confirm(id, authentication.getName());
	}

	@PostMapping("/{id}/ship")
	@Operation(summary = "Mark a confirmed order as shipped")
	public OrderResponse ship(@PathVariable Long id) {
		return orderService.ship(id);
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancel an order; confirmed orders are restocked")
	public OrderResponse cancel(@PathVariable Long id, Authentication authentication) {
		return orderService.cancel(id, authentication.getName());
	}

}
