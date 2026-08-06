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

	/**
	 * Descuenta Tickets de la Disponibilidad de forma pura: devuelve una nueva
	 * proyección con {@code reservedTickets} incrementado. Rechaza cantidades
	 * no positivas y peticiones que superen la Disponibilidad
	 * ({@link SoldOutException}). El descuento se persiste con optimistic
	 * locking (columna {@code version}, ADR-0003): dos Reservas concurrentes
	 * sobre los últimos Tickets, una falla.
	 */
	public EventProjection reserve(int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException("quantity debe ser mayor que 0: " + quantity);
		}
		if (quantity > available()) {
			throw new SoldOutException(id, available(), quantity);
		}
		return new EventProjection(id, name, startsAt, price, capacity, reservedTickets + quantity);
	}
}
