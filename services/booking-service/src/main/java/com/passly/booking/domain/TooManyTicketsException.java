package com.passly.booking.domain;

/**
 * Thrown when a Reservation requests more Tickets than the maximum allowed per
 * Reservation (configurable limit, default 4). Mapped to an RFC 7807 400 error
 * at the web border.
 */
public class TooManyTicketsException extends RuntimeException {

	public TooManyTicketsException(int maxTickets) {
		super("Cannot reserve more than " + maxTickets + " tickets per reservation");
	}
}
