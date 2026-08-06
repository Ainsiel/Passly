package com.passly.booking.application;

/**
 * Se lanza cuando no existe una proyección de Evento para reservar. Se traduce
 * a un error RFC 7807 de tipo 404 en el borde web.
 */
public class EventNotFoundException extends RuntimeException {

	public EventNotFoundException(Long id) {
		super("No existe un evento con id " + id);
	}
}
