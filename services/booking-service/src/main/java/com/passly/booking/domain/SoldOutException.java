package com.passly.booking.domain;

/**
 * Se lanza cuando no hay Disponibilidad suficiente para reservar: el Evento
 * está agotado o solo quedan menos Tickets de los pedidos. Se traduce a un
 * error RFC 7807 de tipo 409 en el borde web.
 */
public class SoldOutException extends RuntimeException {

	public SoldOutException(Long eventId, int available, int requested) {
		super("El evento " + eventId + " está agotado: quedan " + available + " tickets y se piden " + requested);
	}
}
