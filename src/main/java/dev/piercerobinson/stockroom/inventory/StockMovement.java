package dev.piercerobinson.stockroom.inventory;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import dev.piercerobinson.stockroom.product.Product;

/**
 * One line in the stock ledger. Rows are only ever inserted, never updated, so the history of
 * every product's stock level can be audited and replayed.
 */
@Entity
@Table(name = "stock_movement")
public class StockMovement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, updatable = false)
	private Product product;

	@Enumerated(EnumType.STRING)
	@Column(name = "movement_type", nullable = false, updatable = false)
	private MovementType type;

	@Column(name = "quantity_change", nullable = false, updatable = false)
	private int quantityChange;

	@Column(name = "quantity_after", nullable = false, updatable = false)
	private int quantityAfter;

	@Column(updatable = false)
	private String reference;

	@Column(updatable = false)
	private String note;

	@Column(name = "created_by", nullable = false, updatable = false)
	private String createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected StockMovement() {
	}

	StockMovement(Product product, MovementType type, int quantityChange, int quantityAfter, String reference,
			String note, String createdBy) {
		this.product = product;
		this.type = type;
		this.quantityChange = quantityChange;
		this.quantityAfter = quantityAfter;
		this.reference = reference;
		this.note = note;
		this.createdBy = createdBy;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Product getProduct() {
		return product;
	}

	public MovementType getType() {
		return type;
	}

	public int getQuantityChange() {
		return quantityChange;
	}

	public int getQuantityAfter() {
		return quantityAfter;
	}

	public String getReference() {
		return reference;
	}

	public String getNote() {
		return note;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
