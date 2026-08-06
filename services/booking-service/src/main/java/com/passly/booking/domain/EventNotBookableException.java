package com.passly.booking.domain;

import java.time.LocalDateTime;

/**
 * Se lanza cuando se intenta reservar un Evento que ya ha comenzado. Se
 * traduce a un error RFC 7807 de tipo 409 en el borde web.
 */
public class EventNotBookableException extends RuntimeException {

	public EventNotBookableException(Long eventId, LocalDateTime startsAt) {
		super("El evento " + eventId + " ya ha comenzado (" + startsAt + ") y no acepta reservas");
	}
}
