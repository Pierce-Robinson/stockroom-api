package dev.piercerobinson.stockroom.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class OrderDtos {

	private OrderDtos() {
	}

	public record CreateOrderRequest(
			@NotBlank @Size(max = 200) String customerName,
			@NotEmpty @Size(max = 100) List<@Valid @NotNull OrderLineRequest> lines) {
	}

	public record OrderLineRequest(@NotNull Long productId, @NotNull @Positive Integer quantity) {
	}

	public record OrderLineResponse(Long productId, String sku, String productName, int quantity,
			BigDecimal unitPrice, BigDecimal lineTotal) {

		static OrderLineResponse from(OrderLine line) {
			return new OrderLineResponse(line.getProduct().getId(), line.getProduct().getSku(),
					line.getProduct().getName(), line.getQuantity(), line.getUnitPrice(), line.getLineTotal());
		}

	}

	public record OrderResponse(Long id, String orderNumber, String customerName, OrderStatus status,
			BigDecimal totalAmount, List<OrderLineResponse> lines, String createdBy, Instant createdAt,
			Instant confirmedAt, Instant shippedAt, Instant cancelledAt) {

		static OrderResponse from(SalesOrder order) {
			return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCustomerName(),
					order.getStatus(), order.getTotalAmount(),
					order.getLines().stream().map(OrderLineResponse::from).toList(), order.getCreatedBy(),
					order.getCreatedAt(), order.getConfirmedAt(), order.getShippedAt(), order.getCancelledAt());
		}

	}

	/** Lighter shape for list views: no line items. */
	public record OrderSummary(Long id, String orderNumber, String customerName, OrderStatus status,
			BigDecimal totalAmount, Instant createdAt) {

		static OrderSummary from(SalesOrder order) {
			return new OrderSummary(order.getId(), order.getOrderNumber(), order.getCustomerName(),
					order.getStatus(), order.getTotalAmount(), order.getCreatedAt());
		}

	}

}
