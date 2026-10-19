package dev.piercerobinson.stockroom.inventory;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.piercerobinson.stockroom.common.PageResponse;
import dev.piercerobinson.stockroom.inventory.InventoryDtos.AdjustStockRequest;
import dev.piercerobinson.stockroom.inventory.InventoryDtos.ReceiveStockRequest;
import dev.piercerobinson.stockroom.inventory.InventoryDtos.StockMovementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/products/{productId}/stock")
@Tag(name = "Inventory")
public class InventoryController {

	private final InventoryService inventoryService;

	public InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@PostMapping("/receipts")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
	@Operation(summary = "Record goods received")
	public StockMovementResponse receive(@PathVariable Long productId, @Valid @RequestBody ReceiveStockRequest request,
			Authentication authentication) {
		return inventoryService.receive(productId, request, authentication.getName());
	}

	@PostMapping("/adjustments")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Correct stock up or down with a reason (admin)")
	public StockMovementResponse adjust(@PathVariable Long productId, @Valid @RequestBody AdjustStockRequest request,
			Authentication authentication) {
		return inventoryService.adjust(productId, request, authentication.getName());
	}

	@GetMapping("/movements")
	@Operation(summary = "Stock ledger for a product, newest first")
	public PageResponse<StockMovementResponse> history(@PathVariable Long productId,
			@PageableDefault(size = 50) Pageable pageable) {
		return inventoryService.history(productId, pageable);
	}

}
