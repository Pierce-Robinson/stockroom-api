package dev.piercerobinson.stockroom.order;

import java.util.Set;

/**
 * Order lifecycle:
 *
 * <pre>
 * DRAFT ──confirm──▶ CONFIRMED ──ship──▶ SHIPPED
 *   │                    │
 *   └──cancel──▶ CANCELLED ◀──cancel──┘  (restocks)
 * </pre>
 */
public enum OrderStatus {

	DRAFT, CONFIRMED, SHIPPED, CANCELLED;

	public boolean canTransitionTo(OrderStatus target) {
		return switch (this) {
			case DRAFT -> Set.of(CONFIRMED, CANCELLED).contains(target);
			case CONFIRMED -> Set.of(SHIPPED, CANCELLED).contains(target);
			case SHIPPED, CANCELLED -> false;
		};
	}

}
