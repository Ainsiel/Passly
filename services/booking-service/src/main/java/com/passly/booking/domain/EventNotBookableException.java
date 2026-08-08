package com.passly.booking.domain;

import java.time.LocalDateTime;

/**
 * Thrown when attempting to reserve an Event that has already started. Mapped
 * to an RFC 7807 409 error at the web border.
 */
public class EventNotBookableException extends RuntimeException {

	public EventNotBookableException(Long eventId, LocalDateTime startsAt) {
		super("Event " + eventId + " has already started (" + startsAt + ") and does not accept reservations");
	}
}
