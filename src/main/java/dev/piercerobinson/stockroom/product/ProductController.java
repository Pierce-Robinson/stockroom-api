package dev.piercerobinson.stockroom.product;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.piercerobinson.stockroom.common.PageResponse;
import dev.piercerobinson.stockroom.product.ProductDtos.CreateProductRequest;
import dev.piercerobinson.stockroom.product.ProductDtos.ProductResponse;
import dev.piercerobinson.stockroom.product.ProductDtos.UpdateProductRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	@Operation(summary = "Search active products by name or SKU, paginated")
	public PageResponse<ProductResponse> search(@RequestParam(required = false) String search,
			@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
		return productService.search(search, pageable);
	}

	@GetMapping("/low-stock")
	@Operation(summary = "Active products at or below their reorder level")
	public List<ProductResponse> lowStock() {
		return productService.lowStock();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a product")
	public ProductResponse get(@PathVariable Long id) {
		return productService.get(id);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Create a product (admin)")
	public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
		ProductResponse created = productService.create(request);
		return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Update a product (admin). Send the version you read to avoid overwriting changes.")
	public ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
		return productService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Deactivate a product (admin). History is kept.")
	public ResponseEntity<Void> deactivate(@PathVariable Long id) {
		productService.deactivate(id);
		return ResponseEntity.noContent().build();
	}

}
