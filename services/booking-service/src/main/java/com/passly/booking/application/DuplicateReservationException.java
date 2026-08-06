package com.passly.booking.application;

/**
 * Se lanza cuando un Usuario intenta una segunda Reserva activa para el mismo
 * Evento. Se traduce a un error RFC 7807 de tipo 409 en el borde web. La
 * unicidad real la refuerza un índice único parcial en la BD.
 */
public class DuplicateReservationException extends RuntimeException {

	public DuplicateReservationException(String userId, Long eventId) {
		super("El usuario " + userId + " ya tiene una reserva activa para el evento " + eventId);
	}
}
