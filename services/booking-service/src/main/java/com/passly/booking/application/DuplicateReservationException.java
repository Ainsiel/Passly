package com.passly.booking.application;

/**
 * Thrown when a User attempts a second active Reservation for the same Event.
 * Mapped to an RFC 7807 409 error at the web border. Actual uniqueness is
 * enforced by a partial unique index in the DB.
 */
public class DuplicateReservationException extends RuntimeException {

	public DuplicateReservationException(String userId, Long eventId) {
		super("User " + userId + " already has an active reservation for event " + eventId);
	}
}
