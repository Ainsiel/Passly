package com.passly.booking.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.passly.booking.domain.Reservation;
import com.passly.booking.domain.ReservationStatus;
import com.passly.booking.domain.Ticket;

/**
 * Response for a {@link Reservation}: the Event snapshot frozen at purchase time,
 * the recipient email of the Tickets, and the issued Tickets (code + QR payload).
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
