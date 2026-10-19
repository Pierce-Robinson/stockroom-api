package dev.piercerobinson.stockroom.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import dev.piercerobinson.stockroom.common.BusinessRuleException;
import dev.piercerobinson.stockroom.product.Product;

@Entity
@Table(name = "sales_order")
public class SalesOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_number", unique = true, length = 20)
	private String orderNumber;

	@Column(name = "customer_name", nullable = false, length = 200)
	private String customerName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.DRAFT;

	@Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@Column(name = "created_by", nullable = false, updatable = false)
	private String createdBy;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<OrderLine> lines = new ArrayList<>();

	@Version
	private long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "confirmed_at")
	private Instant confirmedAt;

	@Column(name = "shipped_at")
	private Instant shippedAt;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	protected SalesOrder() {
	}

	public SalesOrder(String customerName, String createdBy) {
		this.customerName = customerName;
		this.createdBy = createdBy;
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

	public void addLine(Product product, int quantity) {
		lines.add(new OrderLine(this, product, quantity));
		totalAmount = lines.stream().map(OrderLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/** Called once the database has assigned an id, giving a human-friendly number like SO-000042. */
	void assignOrderNumber() {
		orderNumber = "SO-%06d".formatted(id);
	}

	void markConfirmed() {
		transitionTo(OrderStatus.CONFIRMED);
		confirmedAt = Instant.now();
	}

	void markShipped() {
		transitionTo(OrderStatus.SHIPPED);
		shippedAt = Instant.now();
	}

	void markCancelled() {
		transitionTo(OrderStatus.CANCELLED);
		cancelledAt = Instant.now();
	}

	void ensureCanTransitionTo(OrderStatus target) {
		if (!status.canTransitionTo(target)) {
			throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
					"Order %s cannot move from %s to %s".formatted(orderNumber, status, target));
		}
	}

	private void transitionTo(OrderStatus target) {
		ensureCanTransitionTo(target);
		status = target;
	}

	public Long getId() {
		return id;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public String getCustomerName() {
		return customerName;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public List<OrderLine> getLines() {
		return Collections.unmodifiableList(lines);
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

	public Instant getConfirmedAt() {
		return confirmedAt;
	}

	public Instant getShippedAt() {
		return shippedAt;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}

}
