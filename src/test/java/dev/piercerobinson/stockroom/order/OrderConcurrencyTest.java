package dev.piercerobinson.stockroom.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import dev.piercerobinson.stockroom.common.BusinessRuleException;
import dev.piercerobinson.stockroom.order.OrderDtos.CreateOrderRequest;
import dev.piercerobinson.stockroom.order.OrderDtos.OrderLineRequest;
import dev.piercerobinson.stockroom.support.IntegrationTest;

/**
 * Two orders compete for the last units of stock at the same moment. Row locking must let exactly
 * one of them win, and stock must never go below zero.
 */
class OrderConcurrencyTest extends IntegrationTest {

	@Autowired
	private OrderService orderService;

	@Test
	void concurrentConfirmationsNeverOversell() throws Exception {
		long product = createProduct(uniqueSku("race"), "1.00", 0);
		receiveStock(product, 3);
		int contenders = 4;
		List<Long> orderIds = new ArrayList<>();
		for (int i = 0; i < contenders; i++) {
			orderIds.add(orderService
				.create(new CreateOrderRequest("Customer " + i, List.of(new OrderLineRequest(product, 2))), "test")
				.id());
		}

		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(contenders)) {
			for (Long orderId : orderIds) {
				Callable<Boolean> confirm = () -> {
					start.await();
					try {
						orderService.confirm(orderId, "test");
						return true;
					}
					catch (BusinessRuleException ex) {
						assertThat(ex.getCode()).isEqualTo("INSUFFICIENT_STOCK");
						return false;
					}
				};
				results.add(pool.submit(confirm));
			}
			start.countDown();

			int successes = 0;
			for (Future<Boolean> result : results) {
				if (result.get()) {
					successes++;
				}
			}
			assertThat(successes).as("only one order of 2 fits in 3 units of stock").isEqualTo(1);
		}
		assertThat(stockOf(product)).isEqualTo(1);
	}

}
