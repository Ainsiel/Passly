package com.passly.notification.adapter.in.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Copia local (ADR-0007) del evento {@code ticket-reserved} que publica
 * booking-service: misma forma del wire, mismo id lógico de tipo. La validación
 * es estructural y estricta para que un mensaje malformado falle de forma
 * determinista en la deserialización y, tras agotar el reintento, termine en la
 * dead-letter queue (AC3).
 */
public record TicketReservedMessage(UUID reservationId, String email, String eventName, LocalDateTime startsAt,
		BigDecimal price, List<TicketData> tickets) {

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
