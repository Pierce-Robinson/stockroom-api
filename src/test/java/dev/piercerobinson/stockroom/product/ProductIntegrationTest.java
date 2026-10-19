package dev.piercerobinson.stockroom.product;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import dev.piercerobinson.stockroom.support.IntegrationTest;

class ProductIntegrationTest extends IntegrationTest {

	@Test
	void adminCanCreateAProduct() throws Exception {
		String sku = uniqueSku("new").toLowerCase();
		postJson("/api/products", adminToken(), """
				{"sku": "%s", "name": "Angle grinder", "unitPrice": 799.95, "reorderLevel": 3}
				""".formatted(sku))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/products/")))
			.andExpect(jsonPath("$.sku").value(sku.toUpperCase()))
			.andExpect(jsonPath("$.unitPrice").value(799.95))
			.andExpect(jsonPath("$.quantityOnHand").value(0))
			.andExpect(jsonPath("$.lowStock").value(true));
	}

	@Test
	void skusMustBeUnique() throws Exception {
		String sku = uniqueSku("dup");
		createProduct(sku, "10.00", 0);
		postJson("/api/products", adminToken(), """
				{"sku": "%s", "name": "Another", "unitPrice": 5}
				""".formatted(sku.toLowerCase()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SKU_TAKEN"));
	}

	@Test
	void invalidInputReturnsFieldErrors() throws Exception {
		postJson("/api/products", adminToken(), """
				{"sku": "bad sku!", "name": "", "unitPrice": -1}
				""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Validation failed"))
			.andExpect(jsonPath("$.errors.sku").exists())
			.andExpect(jsonPath("$.errors.name").exists())
			.andExpect(jsonPath("$.errors.unitPrice").exists());
	}

	@Test
	void staffCanReadButNotCreateProducts() throws Exception {
		String token = staffToken();
		getJson("/api/products", token).andExpect(status().isOk());
		postJson("/api/products", token, """
				{"sku": "%s", "name": "Nope", "unitPrice": 1}
				""".formatted(uniqueSku("staff")))
			.andExpect(status().isForbidden());
	}

	@Test
	void searchMatchesNameOrSkuAndIsPaginated() throws Exception {
		String sku = uniqueSku("findme");
		createProduct(sku, "1.00", 0);

		getJson("/api/products?search=" + sku.toLowerCase() + "&size=5", adminToken()).andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].sku").value(sku))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.size").value(5))
			.andExpect(jsonPath("$.page").value(0));
	}

	@Test
	void searchTreatsPercentSignsLiterally() throws Exception {
		createProduct(uniqueSku("pct"), "1.00", 0);
		getJson("/api/products?search=%25%25%25", adminToken()).andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void updatesWithAStaleVersionAreRejected() throws Exception {
		long id = createProduct(uniqueSku("ver"), "10.00", 1);
		String token = adminToken();

		putJson("/api/products/" + id, token, """
				{"name": "First edit", "unitPrice": 11.00, "reorderLevel": 1, "version": 0}
				""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(1));

		putJson("/api/products/" + id, token, """
				{"name": "Edit based on old data", "unitPrice": 12.00, "reorderLevel": 1, "version": 0}
				""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
	}

	@Test
	void deactivatedProductsDropOutOfSearchButKeepTheirHistory() throws Exception {
		String sku = uniqueSku("gone");
		long id = createProduct(sku, "3.00", 0);
		String token = adminToken();

		mvc.perform(delete("/api/products/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isNoContent());

		getJson("/api/products?search=" + sku, token).andExpect(jsonPath("$.totalElements").value(0));
		getJson("/api/products/" + id, token).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void lowStockReportListsProductsAtOrBelowReorderLevel() throws Exception {
		String lowSku = uniqueSku("low");
		String okSku = uniqueSku("ok");
		long low = createProduct(lowSku, "2.00", 10);
		long ok = createProduct(okSku, "2.00", 10);
		receiveStock(low, 10);
		receiveStock(ok, 11);

		getJson("/api/products/low-stock", adminToken()).andExpect(status().isOk())
			.andExpect(jsonPath("$[*].sku", hasItem(lowSku)))
			.andExpect(jsonPath("$[*].sku", not(hasItem(okSku))));
	}

	@Test
	void malformedJsonGetsAClearError() throws Exception {
		postJson("/api/products", adminToken(), "{not json")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Malformed request"));
	}

	@Test
	void unknownProductReturns404() throws Exception {
		getJson("/api/products/999999", adminToken()).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Product 999999 was not found"));
	}

}
