package com.passly.booking.domain;

/**
 * Se lanza cuando una Reserva pide más Tickets que el máximo permitido por
 * Reserva (límite configurable, default 4). Se traduce a un error RFC 7807 de
 * tipo 400 en el borde web.
 */
public class TooManyTicketsException extends RuntimeException {

	public TooManyTicketsException(int maxTickets) {
		super("No se pueden reservar más de " + maxTickets + " tickets por reserva");
	}
}
