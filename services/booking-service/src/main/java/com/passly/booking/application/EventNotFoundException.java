package com.passly.booking.application;

/**
 * Thrown when no Event projection exists for reservation. Mapped to an RFC 7807
 * 404 error at the web border.
 */
public class EventNotFoundException extends RuntimeException {

	public EventNotFoundException(Long id) {
		super("Event not found with id " + id);
	}
}
