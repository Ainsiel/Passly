package com.passly.booking.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.passly.booking.domain.Reservation;
import com.passly.booking.domain.ReservationStatus;
import com.passly.booking.domain.Ticket;

/**
 * Respuesta de una {@link Reservation}: la instantánea del Evento congelada en
 * la compra, el email destinatario de los Tickets y los Tickets emitidos
 * (código + payload QR).
 */
public record ReservationResponse(UUID id, Long eventId, String eventName, LocalDateTime startsAt,
		BigDecimal price, ReservationStatus status, String email, LocalDateTime createdAt,
		List<TicketResponse> tickets) {

	public static ReservationResponse from(Reservation reservation) {
		return new ReservationResponse(reservation.id(), reservation.eventId(), reservation.eventName(),
			reservation.startsAt(), reservation.price(), reservation.status(), reservation.email(),
			reservation.createdAt(), reservation.tickets().stream().map(TicketResponse::from).toList());
	}

	public record TicketResponse(String code, String qr) {

		static TicketResponse from(Ticket ticket) {
			return new TicketResponse(ticket.code(), ticket.qr());
		}
	}
}
