package com.passly.notification.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Copia local (ADR-0007) del evento {@code ticket-reserved} que publica
 * booking-service: misma forma del wire, mismo id lógico de tipo. Vive en la
 * capa de aplicación como contrato de entrada del caso de uso; los adaptadores
 * de entrada (deserialización) y de salida (render/email) dependen de él.
 * La validación es estructural y estricta para que un mensaje malformado falle
 * de forma determinista en la deserialización y, tras agotar el reintento,
 * termine en la dead-letter queue (AC3).
 */
public record TicketReservedMessage(UUID reservationId, String email, Long eventId, String eventName,
		LocalDateTime startsAt, BigDecimal price, Integer reservedTickets, List<TicketData> tickets) {

	public TicketReservedMessage {
		if (reservationId == null || email == null || email.isBlank() || eventName == null || startsAt == null
				|| price == null || tickets == null || tickets.isEmpty()) {
			throw new IllegalArgumentException(
					"reservationId, email, eventName, startsAt, price y tickets son obligatorios");
		}
	}

	public record TicketData(String code, String qr) {
	}
}
