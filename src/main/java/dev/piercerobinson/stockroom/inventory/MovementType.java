package dev.piercerobinson.stockroom.inventory;

public enum MovementType {

	/** Goods received from a supplier. */
	RECEIPT,

	/** Manual correction after a stock count, damage or loss. */
	ADJUSTMENT,

	/** Stock leaving when a sales order is confirmed. */
	SALE,

	/** Stock returned when a confirmed order is cancelled. */
	ORDER_CANCELLED

}
