package com.passly.booking.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proyección de un Evento del contexto Reservas (ADR-0011). Se mantiene
 * sincronizada desde Catálogo vía RabbitMQ: los Eventos no viven aquí, solo su
 * reflejo con Capacidad y Disponibilidad para controlar la concurrencia de las
 * Reservas. La Disponibilidad es una propiedad derivada, nunca gestionable.
 */
public record EventProjection(Long id, String name, LocalDateTime startsAt, BigDecimal price, int capacity,
		int reservedTickets) {

	public EventProjection {
		if (id == null) {
			throw new IllegalArgumentException("id no puede ser null");
		}
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity no puede ser negativo: " + capacity);
		}
		if (reservedTickets < 0 || reservedTickets > capacity) {
			throw new IllegalArgumentException(
				"reservedTickets debe estar entre 0 y capacity: " + reservedTickets);
		}
	}

	public int available() {
		return capacity - reservedTickets;
	}
}
