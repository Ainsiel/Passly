package com.passly.booking.adapter.out.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.passly.booking.domain.Reservation;

/**
 * Forma del mensaje que viaja por RabbitMQ hacia notification-service cuando se
 * crea una Reserva (ticket #8). Lleva la instantánea del Evento congelada en la
 * compra, el email del destinatario y los Tickets emitidos (código + payload
 * QR). Es el contrato del wire que notification refleja en su propia copia.
 */
public record TicketReservedMessage(UUID reservationId, String email, Long eventId, String eventName,
		LocalDateTime startsAt, BigDecimal price, int reservedTickets, List<TicketData> tickets) {

	public TicketReservedMessage {
		if (reservationId == null || email == null || email.isBlank() || eventId == null || eventName == null
				|| startsAt == null || price == null || tickets == null || tickets.isEmpty()) {
			throw new IllegalArgumentException(
				"reservationId, email, eventId, eventName, startsAt, price y tickets son obligatorios");
		}
	}

	public static TicketReservedMessage from(Reservation reservation, int reservedTickets) {
		return new TicketReservedMessage(reservation.id(), reservation.email(), reservation.eventId(),
			reservation.eventName(), reservation.startsAt(), reservation.price(), reservedTickets,
			reservation.tickets().stream().map(ticket -> new TicketData(ticket.code(), ticket.qr())).toList());
	}

	public record TicketData(String code, String qr) {
	}
}
