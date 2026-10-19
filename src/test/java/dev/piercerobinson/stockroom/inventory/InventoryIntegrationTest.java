package dev.piercerobinson.stockroom.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import dev.piercerobinson.stockroom.support.IntegrationTest;

class InventoryIntegrationTest extends IntegrationTest {

	@Test
	void receiptsIncreaseStockAndAreRecordedInTheLedger() throws Exception {
		long id = createProduct(uniqueSku("rcv"), "5.00", 0);

		postJson("/api/products/" + id + "/stock/receipts", staffToken(), """
				{"quantity": 25, "reference": "PO-1001", "note": "Weekly delivery"}
				""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.type").value("RECEIPT"))
			.andExpect(jsonPath("$.quantityChange").value(25))
			.andExpect(jsonPath("$.quantityAfter").value(25))
			.andExpect(jsonPath("$.createdBy").value(STAFF_EMAIL));

		assertThat(stockOf(id)).isEqualTo(25);
		getJson("/api/products/" + id + "/stock/movements", adminToken()).andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].reference").value("PO-1001"));
	}

	@Test
	void adjustmentsCannotTakeStockBelowZero() throws Exception {
		long id = createProduct(uniqueSku("adj"), "5.00", 0);
		receiveStock(id, 3);

		postJson("/api/products/" + id + "/stock/adjustments", adminToken(), """
				{"quantityChange": -4, "reason": "Stock count"}
				""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

		assertThat(stockOf(id)).isEqualTo(3);
	}

	@Test
	void adjustmentsRequireAReasonAndAnAdmin() throws Exception {
		long id = createProduct(uniqueSku("adj2"), "5.00", 0);

		postJson("/api/products/" + id + "/stock/adjustments", adminToken(), """
				{"quantityChange": 2, "reason": ""}
				""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.reason").exists());

		postJson("/api/products/" + id + "/stock/adjustments", staffToken(), """
				{"quantityChange": 2, "reason": "Found extra"}
				""")
			.andExpect(status().isForbidden());
	}

	@Test
	void receiptQuantityMustBePositive() throws Exception {
		long id = createProduct(uniqueSku("neg"), "5.00", 0);
		postJson("/api/products/" + id + "/stock/receipts", adminToken(), """
				{"quantity": 0}
				""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.quantity").exists());
	}

}
