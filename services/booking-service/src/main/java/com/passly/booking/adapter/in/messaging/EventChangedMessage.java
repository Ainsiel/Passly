package com.passly.booking.adapter.in.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Contrato wire {@code catalog -> booking} (ADR-0011). Duplicado
 * deliberadamente en ambos servicios: el type mapping usa el id lógico
 * {@code passly:catalog:event-changed}, nunca el FQCN.
 *
 * @param type  tipo de evento (EventCreated | EventUpdated)
 * @param event instantánea del evento
 */
public record EventChangedMessage(String type, EventData event) {

	/** Instantánea del evento tal y como se conoce en el catálogo. */
	public record EventData(Long id, String name, LocalDateTime startsAt, BigDecimal price, int capacity,
			int reservedTickets) {
	}
}
