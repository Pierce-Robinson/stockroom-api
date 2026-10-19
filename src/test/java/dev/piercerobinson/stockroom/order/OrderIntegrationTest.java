package dev.piercerobinson.stockroom.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.jayway.jsonpath.JsonPath;

import dev.piercerobinson.stockroom.support.IntegrationTest;

class OrderIntegrationTest extends IntegrationTest {

	@Test
	void fullOrderLifecycleMovesStockCorrectly() throws Exception {
		long product = createProduct(uniqueSku("life"), "10.00", 0);
		receiveStock(product, 10);
		String token = staffToken();

		// The same product twice is merged into one line.
		long orderId = idFrom(postJson("/api/orders", token, """
				{"customerName": "Acme Builders", "lines": [
				  {"productId": %d, "quantity": 2},
				  {"productId": %d, "quantity": 1}
				]}
				""".formatted(product, product))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.orderNumber", matchesPattern("SO-\\d{6}")))
			.andExpect(jsonPath("$.lines.length()").value(1))
			.andExpect(jsonPath("$.lines[0].quantity").value(3))
			.andExpect(jsonPath("$.totalAmount").value(30.00)));

		assertThat(stockOf(product)).as("drafts don't touch stock").isEqualTo(10);

		postJson("/api/orders/" + orderId + "/confirm", token, "").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
			.andExpect(jsonPath("$.confirmedAt").isNotEmpty());
		assertThat(stockOf(product)).isEqualTo(7);

		getJson("/api/products/" + product + "/stock/movements", token)
			.andExpect(jsonPath("$.content[0].type").value("SALE"))
			.andExpect(jsonPath("$.content[0].quantityChange").value(-3));

		postJson("/api/orders/" + orderId + "/ship", token, "").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SHIPPED"));

		postJson("/api/orders/" + orderId + "/cancel", token, "").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
		assertThat(stockOf(product)).isEqualTo(7);
	}

	@Test
	void cancellingAConfirmedOrderPutsTheStockBack() throws Exception {
		long product = createProduct(uniqueSku("cxl"), "4.00", 0);
		receiveStock(product, 5);
		String token = staffToken();
		long orderId = createOrder(token, product, 4);

		postJson("/api/orders/" + orderId + "/confirm", token, "").andExpect(status().isOk());
		assertThat(stockOf(product)).isEqualTo(1);

		postJson("/api/orders/" + orderId + "/cancel", token, "").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));
		assertThat(stockOf(product)).isEqualTo(5);
		getJson("/api/products/" + product + "/stock/movements", token)
			.andExpect(jsonPath("$.content[0].type").value("ORDER_CANCELLED"));
	}

	@Test
	void confirmationIsAllOrNothingWhenStockIsShort() throws Exception {
		String shortSku = uniqueSku("short");
		long plenty = createProduct(uniqueSku("plenty"), "1.00", 0);
		long scarce = createProduct(shortSku, "1.00", 0);
		receiveStock(plenty, 5);
		receiveStock(scarce, 1);
		String token = staffToken();

		long orderId = idFrom(postJson("/api/orders", token, """
				{"customerName": "Big Order Ltd", "lines": [
				  {"productId": %d, "quantity": 2},
				  {"productId": %d, "quantity": 3}
				]}
				""".formatted(plenty, scarce)));

		postJson("/api/orders/" + orderId + "/confirm", token, "").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
			.andExpect(jsonPath("$.shortages.length()").value(1))
			.andExpect(jsonPath("$.shortages[0].sku").value(shortSku))
			.andExpect(jsonPath("$.shortages[0].requested").value(3))
			.andExpect(jsonPath("$.shortages[0].available").value(1));

		assertThat(stockOf(plenty)).as("no partial deduction").isEqualTo(5);
		assertThat(stockOf(scarce)).isEqualTo(1);
		getJson("/api/orders/" + orderId, token).andExpect(jsonPath("$.status").value("DRAFT"));
	}

	@Test
	void orderLinesKeepThePriceFromWhenTheOrderWasPlaced() throws Exception {
		long product = createProduct(uniqueSku("price"), "20.00", 0);
		long orderId = createOrder(staffToken(), product, 2);

		putJson("/api/products/" + product, adminToken(), """
				{"name": "Price rise", "unitPrice": 25.00, "reorderLevel": 0, "version": 0}
				""")
			.andExpect(status().isOk());

		getJson("/api/orders/" + orderId, adminToken()).andExpect(jsonPath("$.lines[0].unitPrice").value(20.00))
			.andExpect(jsonPath("$.totalAmount").value(40.00));
	}

	@Test
	void inactiveProductsCannotBeOrdered() throws Exception {
		long product = createProduct(uniqueSku("inact"), "1.00", 0);
		mvc.perform(delete("/api/products/" + product).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
			.andExpect(status().isNoContent());

		postJson("/api/orders", staffToken(), """
				{"customerName": "Someone", "lines": [{"productId": %d, "quantity": 1}]}
				""".formatted(product))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PRODUCT_INACTIVE"));
	}

	@Test
	void ordersNeedACustomerAndAtLeastOneValidLine() throws Exception {
		postJson("/api/orders", staffToken(), """
				{"customerName": "", "lines": []}
				""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.customerName").exists())
			.andExpect(jsonPath("$.errors.lines").exists());
	}

	@Test
	void draftOrdersCannotBeShipped() throws Exception {
		long product = createProduct(uniqueSku("ship"), "1.00", 0);
		String token = staffToken();
		long orderId = createOrder(token, product, 1);

		postJson("/api/orders/" + orderId + "/ship", token, "").andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
	}

	@Test
	void ordersCanBeFilteredByStatus() throws Exception {
		long product = createProduct(uniqueSku("filter"), "1.00", 0);
		receiveStock(product, 1);
		String token = staffToken();
		long draftId = createOrder(token, product, 1);
		long confirmedId = createOrder(token, product, 1);
		postJson("/api/orders/" + confirmedId + "/confirm", token, "").andExpect(status().isOk());

		getJson("/api/orders?status=CONFIRMED&size=100", token).andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id", hasItem((int) confirmedId)))
			.andExpect(jsonPath("$.content[*].id", not(hasItem((int) draftId))));
	}

	@Test
	void unknownOrdersReturn404() throws Exception {
		getJson("/api/orders/999999", staffToken()).andExpect(status().isNotFound());
	}

	private long createOrder(String token, long productId, int quantity) throws Exception {
		String body = postJson("/api/orders", token, """
				{"customerName": "Test Customer", "lines": [{"productId": %d, "quantity": %d}]}
				""".formatted(productId, quantity))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

}
