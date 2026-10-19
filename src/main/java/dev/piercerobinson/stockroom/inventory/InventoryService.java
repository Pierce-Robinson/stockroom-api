package dev.piercerobinson.stockroom.inventory;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import dev.piercerobinson.stockroom.common.BusinessRuleException;
import dev.piercerobinson.stockroom.common.NotFoundException;
import dev.piercerobinson.stockroom.common.PageResponse;
import dev.piercerobinson.stockroom.inventory.InventoryDtos.AdjustStockRequest;
import dev.piercerobinson.stockroom.inventory.InventoryDtos.ReceiveStockRequest;
import dev.piercerobinson.stockroom.inventory.InventoryDtos.StockMovementResponse;
import dev.piercerobinson.stockroom.product.Product;
import dev.piercerobinson.stockroom.product.ProductRepository;

/**
 * The only place stock levels change. Every change updates the product's quantity and writes a
 * ledger row in the same transaction, so the two can never drift apart.
 */
@Service
public class InventoryService {

	private final ProductRepository products;

	private final StockMovementRepository movements;

	public InventoryService(ProductRepository products, StockMovementRepository movements) {
		this.products = products;
		this.movements = movements;
	}

	@Transactional
	public StockMovementResponse receive(Long productId, ReceiveStockRequest request, String actor) {
		Product product = findActiveForUpdate(productId);
		return StockMovementResponse.from(record(product, MovementType.RECEIPT, request.quantity(),
				request.reference(), request.note(), actor));
	}

	@Transactional
	public StockMovementResponse adjust(Long productId, AdjustStockRequest request, String actor) {
		if (request.quantityChange() == 0) {
			throw new BusinessRuleException("ZERO_ADJUSTMENT", "An adjustment must change the quantity");
		}
		Product product = findActiveForUpdate(productId);
		return StockMovementResponse.from(record(product, MovementType.ADJUSTMENT, request.quantityChange(), null,
				request.reason(), actor));
	}

	@Transactional(readOnly = true)
	public PageResponse<StockMovementResponse> history(Long productId, Pageable pageable) {
		if (!products.existsById(productId)) {
			throw new NotFoundException("Product", productId);
		}
		return PageResponse.from(movements.findByProductIdOrderByCreatedAtDescIdDesc(productId, pageable),
				StockMovementResponse::from);
	}

	/**
	 * Applies a stock change to a product the caller has already loaded (and locked) inside its
	 * own transaction. Used by order processing.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public StockMovement record(Product product, MovementType type, int quantityChange, String reference,
			String note, String actor) {
		int quantityAfter = product.changeStock(quantityChange);
		return movements.save(new StockMovement(product, type, quantityChange, quantityAfter, reference, note, actor));
	}

	private Product findActiveForUpdate(Long productId) {
		Product product = products.findAllByIdForUpdate(List.of(productId))
			.stream()
			.findFirst()
			.orElseThrow(() -> new NotFoundException("Product", productId));
		if (!product.isActive()) {
			throw new BusinessRuleException("PRODUCT_INACTIVE", "Product %s is inactive".formatted(product.getSku()));
		}
		return product;
	}

}
