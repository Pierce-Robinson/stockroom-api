package dev.piercerobinson.stockroom.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderStatusTest {

	@ParameterizedTest(name = "{0} -> {1} allowed: {2}")
	@CsvSource({
			"DRAFT, CONFIRMED, true",
			"DRAFT, CANCELLED, true",
			"DRAFT, SHIPPED, false",
			"CONFIRMED, SHIPPED, true",
			"CONFIRMED, CANCELLED, true",
			"CONFIRMED, CONFIRMED, false",
			"CONFIRMED, DRAFT, false",
			"SHIPPED, CANCELLED, false",
			"SHIPPED, DRAFT, false",
			"CANCELLED, CONFIRMED, false",
			"CANCELLED, DRAFT, false" })
	void transitions(OrderStatus from, OrderStatus to, boolean allowed) {
		assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
	}

}
