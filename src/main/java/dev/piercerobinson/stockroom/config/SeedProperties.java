package dev.piercerobinson.stockroom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(boolean enabled, boolean demoData, String adminEmail, String adminPassword,
		String staffEmail, String staffPassword) {
}
