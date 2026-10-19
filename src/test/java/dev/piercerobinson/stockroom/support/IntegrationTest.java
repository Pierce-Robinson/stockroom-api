package dev.piercerobinson.stockroom.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * Base class for end-to-end API tests: boots the full app on an in-memory database and drives it
 * through real HTTP requests with real JWTs. Tests create their own uniquely named data, so they
 * never depend on each other or on run order.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

	protected static final String ADMIN_EMAIL = "admin@stockroom.local";

	protected static final String ADMIN_PASSWORD = "Admin123!";

	protected static final String STAFF_EMAIL = "staff@stockroom.local";

	protected static final String STAFF_PASSWORD = "Staff123!";

	@Autowired
	protected MockMvc mvc;

	protected String adminToken() throws Exception {
		return login(ADMIN_EMAIL, ADMIN_PASSWORD);
	}

	protected String staffToken() throws Exception {
		return login(STAFF_EMAIL, STAFF_PASSWORD);
	}

	protected String login(String email, String password) throws Exception {
		String body = mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s", "password": "%s"}
						""".formatted(email, password)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	protected ResultActions getJson(String path, String token) throws Exception {
		return mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	protected ResultActions postJson(String path, String token, String json) throws Exception {
		return mvc.perform(post(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	protected ResultActions putJson(String path, String token, String json) throws Exception {
		return mvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	protected static String uniqueSku(String prefix) {
		return (prefix + "-" + UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
	}

	/** Creates a product as admin and returns its id. */
	protected long createProduct(String sku, String price, int reorderLevel) throws Exception {
		String body = postJson("/api/products", adminToken(), """
				{"sku": "%s", "name": "Test product %s", "unitPrice": %s, "reorderLevel": %d}
				""".formatted(sku, sku, price, reorderLevel))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

	protected void receiveStock(long productId, int quantity) throws Exception {
		postJson("/api/products/" + productId + "/stock/receipts", adminToken(), """
				{"quantity": %d, "reference": "PO-TEST"}
				""".formatted(quantity))
			.andExpect(status().isCreated());
	}

	protected int stockOf(long productId) throws Exception {
		String body = getJson("/api/products/" + productId, adminToken()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.quantityOnHand");
	}

	protected static long idFrom(ResultActions result) throws Exception {
		return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

}
