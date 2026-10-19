package dev.piercerobinson.stockroom.product;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import dev.piercerobinson.stockroom.common.BusinessRuleException;

@Entity
@Table(name = "product")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 40)
	private String sku;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "quantity_on_hand", nullable = false)
	private int quantityOnHand;

	@Column(name = "reorder_level", nullable = false)
	private int reorderLevel;

	@Column(nullable = false)
	private boolean active = true;

	/** Optimistic locking: stops two people silently overwriting each other's edits. */
	@Version
	private long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Product() {
	}

	public Product(String sku, String name, String description, BigDecimal unitPrice, int reorderLevel) {
		this.sku = sku;
		this.name = name;
		this.description = description;
		this.unitPrice = unitPrice;
		this.reorderLevel = reorderLevel;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	/**
	 * Applies a stock change and returns the new quantity. Stock can never go negative; the
	 * database enforces this too with a check constraint.
	 */
	public int changeStock(int delta) {
		int newQuantity = quantityOnHand + delta;
		if (newQuantity < 0) {
			throw new BusinessRuleException("INSUFFICIENT_STOCK",
					"Not enough stock for %s: %d on hand, %d requested".formatted(sku, quantityOnHand, -delta));
		}
		quantityOnHand = newQuantity;
		return newQuantity;
	}

	public boolean isLowStock() {
		return quantityOnHand <= reorderLevel;
	}

	public void updateDetails(String name, String description, BigDecimal unitPrice, int reorderLevel) {
		this.name = name;
		this.description = description;
		this.unitPrice = unitPrice;
		this.reorderLevel = reorderLevel;
	}

	public void deactivate() {
		active = false;
	}

	public Long getId() {
		return id;
	}

	public String getSku() {
		return sku;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public int getQuantityOnHand() {
		return quantityOnHand;
	}

	public int getReorderLevel() {
		return reorderLevel;
	}

	public boolean isActive() {
		return active;
	}

	public long getVersion() {
		return version;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
