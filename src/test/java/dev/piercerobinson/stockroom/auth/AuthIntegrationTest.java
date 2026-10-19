package dev.piercerobinson.stockroom.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import dev.piercerobinson.stockroom.support.IntegrationTest;

class AuthIntegrationTest extends IntegrationTest {

	@Test
	void loginReturnsBearerTokenWithRoles() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "admin@stockroom.local", "password": "Admin123!"}
				"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(3600))
			.andExpect(jsonPath("$.roles[0]").value("ADMIN"));
	}

	@Test
	void wrongPasswordIsRejected() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "admin@stockroom.local", "password": "wrong-password"}
				"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.title").value("Authentication failed"));
	}

	@Test
	void unknownUserGetsTheSameErrorAsWrongPassword() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "nobody@stockroom.local", "password": "whatever1"}
				"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid email or password."));
	}

	@Test
	void requestsWithoutATokenAreRejected() throws Exception {
		mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
	}

	@Test
	void tamperedTokensAreRejected() throws Exception {
		String token = adminToken();
		String tampered = token.substring(0, token.length() - 4) + "abcd";
		mvc.perform(get("/api/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void healthEndpointIsPublic() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void staffCannotManageUsers() throws Exception {
		getJson("/api/users", staffToken()).andExpect(status().isForbidden());
	}

	@Test
	void disabledUsersCannotLogIn() throws Exception {
		String email = "temp-" + System.nanoTime() + "@stockroom.local";
		long userId = idFrom(postJson("/api/users", adminToken(), """
				{"email": "%s", "password": "Password1!", "fullName": "Temp User", "role": "STAFF"}
				""".formatted(email))
			.andExpect(status().isCreated()));

		login(email, "Password1!");
		postJson("/api/users/" + userId + "/disable", adminToken(), "").andExpect(status().isOk());

		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"email": "%s", "password": "Password1!"}
				""".formatted(email)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void duplicateEmailsAreRejected() throws Exception {
		postJson("/api/users", adminToken(), """
				{"email": "STAFF@stockroom.local", "password": "Password1!", "fullName": "Dup", "role": "STAFF"}
				""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
	}

}
