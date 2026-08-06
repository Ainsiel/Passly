package com.passly.catalog.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agregado del contexto Catálogo. La Disponibilidad es una propiedad derivada
 * (Capacity menos reservedTickets); nunca es un estado gestionable.
 */
public record Event(Long id, String name, String description, EventCategory category, String venue,
		LocalDateTime startsAt, BigDecimal price, int capacity, int reservedTickets) {

	public Event {
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
