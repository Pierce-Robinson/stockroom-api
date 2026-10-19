package dev.piercerobinson.stockroom.product;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class ProductDtos {

	private ProductDtos() {
	}

	public record CreateProductRequest(
			@NotBlank @Size(max = 40)
			@Pattern(regexp = "[A-Za-z0-9-]+", message = "may only contain letters, numbers and dashes") String sku,
			@NotBlank @Size(max = 200) String name,
			@Size(max = 2000) String description,
			@NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
			@PositiveOrZero Integer reorderLevel) {

		/** Reorder level is optional when creating a product; it defaults to 0. */
		public int reorderLevelOrDefault() {
			return reorderLevel == null ? 0 : reorderLevel;
		}

	}

	public record UpdateProductRequest(
			@NotBlank @Size(max = 200) String name,
			@Size(max = 2000) String description,
			@NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
			@NotNull @PositiveOrZero Integer reorderLevel,
			@NotNull(message = "is required for optimistic locking") Long version) {
	}

	public record ProductResponse(Long id, String sku, String name, String description, BigDecimal unitPrice,
			int quantityOnHand, int reorderLevel, boolean lowStock, boolean active, long version, Instant createdAt,
			Instant updatedAt) {

		public static ProductResponse from(Product p) {
			return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getUnitPrice(),
					p.getQuantityOnHand(), p.getReorderLevel(), p.isLowStock(), p.isActive(), p.getVersion(),
					p.getCreatedAt(), p.getUpdatedAt());
		}

	}

}
