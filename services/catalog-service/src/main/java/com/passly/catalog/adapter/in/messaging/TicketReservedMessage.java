package com.passly.catalog.adapter.in.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Copia local (ADR-0007) del evento {@code ticket-reserved} que publica
 * booking-service. El catalog-service solo usa eventId y reservedTickets para
 * sincronizar la disponibilidad; el resto del payload se ignora.
 */
public record TicketReservedMessage(UUID reservationId, String email, Long eventId, String eventName,
		LocalDateTime startsAt, BigDecimal price, Integer reservedTickets, List<TicketData> tickets) {

	public record TicketData(String code, String qr) {
	}
}
