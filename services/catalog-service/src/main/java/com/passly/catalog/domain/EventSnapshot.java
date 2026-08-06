package com.passly.catalog.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Instantánea de un Evento que viaja entre contextos (Catálogo → Reservas).
 * Contiene solo los campos que la proyección de Reservas necesita para
 * mostrar Capacidad y Disponibilidad, no el agregado completo.
 */
public record EventSnapshot(Long id, String name, LocalDateTime startsAt, BigDecimal price, int capacity,
		int reservedTickets) {

	public EventSnapshot {
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

	public static EventSnapshot from(Event event) {
		return new EventSnapshot(event.id(), event.name(), event.startsAt(), event.price(), event.capacity(),
			event.reservedTickets());
	}
}
