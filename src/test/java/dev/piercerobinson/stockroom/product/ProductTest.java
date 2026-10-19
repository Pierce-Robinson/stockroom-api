package dev.piercerobinson.stockroom.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import dev.piercerobinson.stockroom.common.BusinessRuleException;

class ProductTest {

	private final Product product = new Product("TEST-1", "Test", null, new BigDecimal("9.99"), 5);

	@Test
	void stockChangesReturnTheNewQuantity() {
		assertThat(product.changeStock(10)).isEqualTo(10);
		assertThat(product.changeStock(-4)).isEqualTo(6);
		assertThat(product.getQuantityOnHand()).isEqualTo(6);
	}

	@Test
	void stockCannotGoNegative() {
		product.changeStock(2);
		assertThatThrownBy(() -> product.changeStock(-3)).isInstanceOf(BusinessRuleException.class)
			.hasMessageContaining("2 on hand, 3 requested");
		assertThat(product.getQuantityOnHand()).isEqualTo(2);
	}

	@Test
	void lowStockIsAtOrBelowTheReorderLevel() {
		product.changeStock(5);
		assertThat(product.isLowStock()).isTrue();
		product.changeStock(1);
		assertThat(product.isLowStock()).isFalse();
	}

}
