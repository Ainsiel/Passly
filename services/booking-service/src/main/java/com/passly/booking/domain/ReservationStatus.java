package com.passly.booking.domain;

/**
 * Estado de una {@link Reservation}. Solo existe {@code ACTIVE} en el MVP: la
 * cancelación está fuera de scope. La unicidad de "una Reserva activa por
 * (Usuario, Evento)" se apoya en este estado (índice parcial en la BD).
 */
public enum ReservationStatus {
	ACTIVE
}
