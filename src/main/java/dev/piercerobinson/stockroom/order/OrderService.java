package dev.piercerobinson.stockroom.order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piercerobinson.stockroom.common.BusinessRuleException;
import dev.piercerobinson.stockroom.common.NotFoundException;
import dev.piercerobinson.stockroom.common.PageResponse;
import dev.piercerobinson.stockroom.inventory.InventoryService;
import dev.piercerobinson.stockroom.inventory.MovementType;
import dev.piercerobinson.stockroom.order.OrderDtos.CreateOrderRequest;
import dev.piercerobinson.stockroom.order.OrderDtos.OrderLineRequest;
import dev.piercerobinson.stockroom.order.OrderDtos.OrderResponse;
import dev.piercerobinson.stockroom.order.OrderDtos.OrderSummary;
import dev.piercerobinson.stockroom.product.Product;
import dev.piercerobinson.stockroom.product.ProductRepository;

@Service
public class OrderService {

	private final SalesOrderRepository orders;

	private final ProductRepository products;

	private final InventoryService inventory;

	public OrderService(SalesOrderRepository orders, ProductRepository products, InventoryService inventory) {
		this.orders = orders;
		this.products = products;
		this.inventory = inventory;
	}

	/** Creates a DRAFT order. Prices are captured now; stock is not touched until confirmation. */
	@Transactional
	public OrderResponse create(CreateOrderRequest request, String actor) {
		Map<Long, Integer> quantities = mergeLines(request.lines());
		Map<Long, Product> found = products.findAllById(quantities.keySet())
			.stream()
			.collect(Collectors.toMap(Product::getId, Function.identity()));

		SalesOrder order = new SalesOrder(request.customerName().trim(), actor);
		quantities.forEach((productId, quantity) -> {
			Product product = found.get(productId);
			if (product == null) {
				throw new NotFoundException("Product", productId);
			}
			if (!product.isActive()) {
				throw new BusinessRuleException("PRODUCT_INACTIVE",
						"Product %s is inactive and cannot be ordered".formatted(product.getSku()));
			}
			order.addLine(product, quantity);
		});

		orders.saveAndFlush(order);
		order.assignOrderNumber();
		return OrderResponse.from(order);
	}

	/**
	 * Confirms an order and takes the stock. All product rows are locked first, then every line is
	 * checked, so the order either takes all of its stock or none of it.
	 */
	@Transactional
	public OrderResponse confirm(Long id, String actor) {
		SalesOrder order = findForUpdate(id);
		order.ensureCanTransitionTo(OrderStatus.CONFIRMED);

		Map<Long, Product> locked = lockProducts(order);
		List<Map<String, Object>> shortages = new ArrayList<>();
		for (OrderLine line : order.getLines()) {
			Product product = locked.get(line.getProduct().getId());
			if (product.getQuantityOnHand() < line.getQuantity()) {
				Map<String, Object> shortage = new LinkedHashMap<>();
				shortage.put("sku", product.getSku());
				shortage.put("requested", line.getQuantity());
				shortage.put("available", product.getQuantityOnHand());
				shortages.add(shortage);
			}
		}
		if (!shortages.isEmpty()) {
			throw new BusinessRuleException("INSUFFICIENT_STOCK",
					"Order %s cannot be confirmed: not enough stock".formatted(order.getOrderNumber()),
					Map.of("shortages", shortages));
		}

		for (OrderLine line : order.getLines()) {
			inventory.record(locked.get(line.getProduct().getId()), MovementType.SALE, -line.getQuantity(),
					order.getOrderNumber(), null, actor);
		}
		order.markConfirmed();
		return OrderResponse.from(order);
	}

	@Transactional
	public OrderResponse ship(Long id) {
		SalesOrder order = findForUpdate(id);
		order.markShipped();
		return OrderResponse.from(order);
	}

	/** Cancels an order. If stock was already taken (CONFIRMED), it is put back. */
	@Transactional
	public OrderResponse cancel(Long id, String actor) {
		SalesOrder order = findForUpdate(id);
		boolean restock = order.getStatus() == OrderStatus.CONFIRMED;
		order.markCancelled();
		if (restock) {
			Map<Long, Product> locked = lockProducts(order);
			for (OrderLine line : order.getLines()) {
				inventory.record(locked.get(line.getProduct().getId()), MovementType.ORDER_CANCELLED,
						line.getQuantity(), order.getOrderNumber(), "Order cancelled", actor);
			}
		}
		return OrderResponse.from(order);
	}

	@Transactional(readOnly = true)
	public OrderResponse get(Long id) {
		return OrderResponse.from(find(id));
	}

	@Transactional(readOnly = true)
	public PageResponse<OrderSummary> list(OrderStatus status, Pageable pageable) {
		var page = status == null ? orders.findAll(pageable) : orders.findByStatus(status, pageable);
		return PageResponse.from(page, OrderSummary::from);
	}

	private SalesOrder find(Long id) {
		return orders.findWithLinesById(id).orElseThrow(() -> new NotFoundException("Order", id));
	}

	private SalesOrder findForUpdate(Long id) {
		return orders.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Order", id));
	}

	private Map<Long, Product> lockProducts(SalesOrder order) {
		List<Long> ids = order.getLines().stream().map(line -> line.getProduct().getId()).toList();
		return products.findAllByIdForUpdate(ids)
			.stream()
			.collect(Collectors.toMap(Product::getId, Function.identity()));
	}

	/** Combines repeated products into one line, keeping the order the client sent them in. */
	private static Map<Long, Integer> mergeLines(List<OrderLineRequest> lines) {
		Map<Long, Integer> merged = new LinkedHashMap<>();
		for (OrderLineRequest line : lines) {
			merged.merge(line.productId(), line.quantity(), Integer::sum);
		}
		return merged;
	}

}
