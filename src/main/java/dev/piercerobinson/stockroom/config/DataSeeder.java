package dev.piercerobinson.stockroom.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.piercerobinson.stockroom.inventory.InventoryDtos.ReceiveStockRequest;
import dev.piercerobinson.stockroom.inventory.InventoryService;
import dev.piercerobinson.stockroom.product.Product;
import dev.piercerobinson.stockroom.product.ProductRepository;
import dev.piercerobinson.stockroom.user.AppUser;
import dev.piercerobinson.stockroom.user.Role;
import dev.piercerobinson.stockroom.user.UserRepository;

/**
 * Creates starter accounts (and optionally demo products) on an empty database, so the API can
 * be tried straight away. Disable with app.seed.enabled=false.
 */
@Component
@ConditionalOnBooleanProperty(name = "app.seed.enabled")
class DataSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	private final SeedProperties seed;

	private final UserRepository users;

	private final ProductRepository products;

	private final InventoryService inventory;

	private final PasswordEncoder passwordEncoder;

	DataSeeder(SeedProperties seed, UserRepository users, ProductRepository products, InventoryService inventory,
			PasswordEncoder passwordEncoder) {
		this.seed = seed;
		this.users = users;
		this.products = products;
		this.inventory = inventory;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (users.count() == 0) {
			users.save(new AppUser(seed.adminEmail(), passwordEncoder.encode(seed.adminPassword()), "Admin User",
					Role.ADMIN));
			users.save(new AppUser(seed.staffEmail(), passwordEncoder.encode(seed.staffPassword()), "Staff User",
					Role.STAFF));
			log.info("Seeded users {} (ADMIN) and {} (STAFF)", seed.adminEmail(), seed.staffEmail());
		}
		if (seed.demoData() && products.count() == 0) {
			seedProduct("BOLT-M8-50", "M8 x 50mm hex bolt (box of 100)", "18.50", 20, 120);
			seedProduct("NUT-M8", "M8 hex nut (box of 100)", "9.90", 20, 15);
			seedProduct("DRILL-18V", "18V cordless drill", "1299.00", 5, 12);
			seedProduct("GLOVE-L", "Work gloves, large", "45.00", 30, 8);
			seedProduct("TAPE-5M", "5m measuring tape", "89.99", 10, 40);
			log.info("Seeded demo products");
		}
	}

	private void seedProduct(String sku, String name, String price, int reorderLevel, int openingStock) {
		Product product = products.save(new Product(sku, name, null, new BigDecimal(price), reorderLevel));
		inventory.receive(product.getId(), new ReceiveStockRequest(openingStock, "OPENING", "Opening stock"),
				"system");
	}

}
