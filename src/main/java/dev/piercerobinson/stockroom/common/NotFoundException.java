package dev.piercerobinson.stockroom.common;

/** Thrown when a requested resource does not exist. Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String resource, Object id) {
		super("%s %s was not found".formatted(resource, id));
	}

}
