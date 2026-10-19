package dev.piercerobinson.stockroom.common;

import java.util.Map;

/**
 * Thrown when a request is valid but breaks a business rule, such as confirming an order
 * without enough stock. Mapped to HTTP 409 with a machine-readable code.
 */
public class BusinessRuleException extends RuntimeException {

	private final String code;

	private final Map<String, Object> details;

	public BusinessRuleException(String code, String message) {
		this(code, message, Map.of());
	}

	public BusinessRuleException(String code, String message, Map<String, Object> details) {
		super(message);
		this.code = code;
		this.details = details;
	}

	public String getCode() {
		return code;
	}

	public Map<String, Object> getDetails() {
		return details;
	}

}
