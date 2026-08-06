package com.passly.catalog.application;

/**
 * Se lanza cuando un Evento solicitado no existe en el catálogo.
 * Se traduce a un error RFC 7807 de tipo 404 en el borde web.
 */
public class EventNotFoundException extends RuntimeException {

	public EventNotFoundException(Long id) {
		super("No existe un evento con id " + id);
	}
}
