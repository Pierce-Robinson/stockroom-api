package dev.piercerobinson.stockroom.inventory;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class InventoryDtos {

	private InventoryDtos() {
	}

	public record ReceiveStockRequest(
			@NotNull @Positive Integer quantity,
			@Size(max = 100) String reference,
			@Size(max = 500) String note) {
	}

	public record AdjustStockRequest(
			@NotNull Integer quantityChange,
			@NotBlank(message = "is required for adjustments") @Size(max = 500) String reason) {
	}

	public record StockMovementResponse(Long id, Long productId, String sku, MovementType type, int quantityChange,
			int quantityAfter, String reference, String note, String createdBy, Instant createdAt) {

		static StockMovementResponse from(StockMovement m) {
			return new StockMovementResponse(m.getId(), m.getProduct().getId(), m.getProduct().getSku(), m.getType(),
					m.getQuantityChange(), m.getQuantityAfter(), m.getReference(), m.getNote(), m.getCreatedBy(),
					m.getCreatedAt());
		}

	}

}
