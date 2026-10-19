package dev.piercerobinson.stockroom.product;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dev.piercerobinson.stockroom.common.BusinessRuleException;
import dev.piercerobinson.stockroom.common.NotFoundException;
import dev.piercerobinson.stockroom.common.PageResponse;
import dev.piercerobinson.stockroom.product.ProductDtos.CreateProductRequest;
import dev.piercerobinson.stockroom.product.ProductDtos.ProductResponse;
import dev.piercerobinson.stockroom.product.ProductDtos.UpdateProductRequest;

@Service
public class ProductService {

	private final ProductRepository products;

	public ProductService(ProductRepository products) {
		this.products = products;
	}

	@Transactional
	public ProductResponse create(CreateProductRequest request) {
		String sku = request.sku().trim().toUpperCase();
		if (products.existsBySkuIgnoreCase(sku)) {
			throw new BusinessRuleException("SKU_TAKEN", "A product with SKU %s already exists".formatted(sku));
		}
		Product product = new Product(sku, request.name().trim(), request.description(), request.unitPrice(),
				request.reorderLevelOrDefault());
		return ProductResponse.from(products.save(product));
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> search(String search, Pageable pageable) {
		if (!StringUtils.hasText(search)) {
			return PageResponse.from(products.findByActiveTrue(pageable), ProductResponse::from);
		}
		String pattern = "%" + escapeLike(search.trim().toLowerCase()) + "%";
		return PageResponse.from(products.searchActive(pattern, pageable), ProductResponse::from);
	}

	@Transactional(readOnly = true)
	public ProductResponse get(Long id) {
		return ProductResponse.from(find(id));
	}

	@Transactional
	public ProductResponse update(Long id, UpdateProductRequest request) {
		Product product = find(id);
		if (product.getVersion() != request.version()) {
			throw new ObjectOptimisticLockingFailureException(Product.class, id);
		}
		product.updateDetails(request.name().trim(), request.description(), request.unitPrice(),
				request.reorderLevel());
		products.flush();
		return ProductResponse.from(product);
	}

	/** Soft delete: products stay in the database because past orders and stock history refer to them. */
	@Transactional
	public void deactivate(Long id) {
		find(id).deactivate();
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> lowStock() {
		return products.findLowStock().stream().map(ProductResponse::from).toList();
	}

	/** Stops user input like "50%" being treated as a wildcard. */
	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	Product find(Long id) {
		return products.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
	}

}
