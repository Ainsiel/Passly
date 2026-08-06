package com.passly.catalog.application;

/**
 * Se lanza cuando una operación de escritura choca con el estado actual de un
 * Evento (p.ej. bajar la Capacidad por debajo de los tickets reservados, o
 * eliminar un evento con reservas). Se traduce a un error RFC 7807 de tipo 409.
 */
public class EventConflictException extends RuntimeException {

	public EventConflictException(String message) {
		super(message);
	}
}
