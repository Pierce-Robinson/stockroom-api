package dev.piercerobinson.stockroom.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

	private static final String BEARER = "bearerAuth";

	@Bean
	OpenAPI stockroomOpenApi() {
		return new OpenAPI()
			.info(new Info().title("Stockroom API")
				.version("1.0")
				.description("Inventory and order management. Log in via POST /api/auth/login, then click "
						+ "Authorize and paste the access token."))
			.components(new Components().addSecuritySchemes(BEARER,
					new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(BEARER))
			// Listed here so Swagger UI shows sections in a logical order.
			.tags(List.of(tag("Authentication", "Log in to get a JWT"), tag("Products", "Product catalogue"),
					tag("Inventory", "Stock receipts, adjustments and history"),
					tag("Orders", "Sales orders: draft, confirm (takes stock), ship, cancel (restocks)"),
					tag("Users", "User management (admin only)")));
	}

	private static Tag tag(String name, String description) {
		return new Tag().name(name).description(description);
	}

}
