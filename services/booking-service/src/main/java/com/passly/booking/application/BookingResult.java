package com.passly.booking.application;

import com.passly.booking.domain.Reservation;

/**
 * Resultado de {@link ReservationService#book}: la Reserva y si fue creada o
 * recuperada por una idempotency key repetida (reenvío del cliente).
 */
public record BookingResult(Reservation reservation, boolean replayed) {

	public static BookingResult created(Reservation reservation) {
		return new BookingResult(reservation, false);
	}

	public static BookingResult replayed(Reservation reservation) {
		return new BookingResult(reservation, true);
	}
}
