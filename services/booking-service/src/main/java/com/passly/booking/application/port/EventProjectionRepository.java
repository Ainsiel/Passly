package com.passly.booking.application.port;

import java.util.Optional;

import com.passly.booking.domain.EventProjection;

/**
 * Puerto de salida de la proyección de eventos. El upsert debe ser idempotente
 * (aplicar el mismo {@link EventProjection} varias veces deja el mismo estado
 * final). {@code reserve} descuenta Disponibilidad con optimistic locking
 * (ADR-0003): lee la fila, valida y actualiza la misma instancia gestionada,
 * de modo que el flush final falle con conflicto si otra Reserva concurrente
 * ya ha cambiado la versión.
 */
public interface EventProjectionRepository {

	void upsert(EventProjection projection);

	/**
	 * Reserva {@code quantity} Tickets de la Disponibilidad del Evento. Devuelve
	 * la proyección con la Disponibilidad descontada, o vacío si el Evento no
	 * tiene proyección. Lanza {@code SoldOutException} si no hay suficiente.
	 */
	Optional<EventProjection> reserve(Long eventId, int quantity);
}
