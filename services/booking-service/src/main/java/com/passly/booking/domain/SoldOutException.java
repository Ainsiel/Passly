package com.passly.booking.domain;

/**
 * Thrown when there is not enough Availability to reserve: the Event is sold
 * out or fewer Tickets remain than requested. Mapped to an RFC 7807 409 error
 * at the web border.
 */
public class SoldOutException extends RuntimeException {

	public SoldOutException(Long eventId, int available, int requested) {
		super("Event " + eventId + " is sold out: " + available + " remaining, " + requested + " requested");
	}
}
