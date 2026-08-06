package com.passly.booking.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Agregado del contexto Reservas: vincula un Usuario con un Evento y contiene
 * uno o más {@link Ticket}s. Máximo una Reserva activa por (Usuario, Evento)
 * (invariante garantizado en la BD con un índice único parcial y aquí con las
 * reglas de {@code book}). La Disponibilidad se descuenta sobre la
 * {@link EventProjection} (optimistic locking, ADR-0003), no en el agregado:
 * este solo valida las invariantes de la Reserva y congela la instantánea del
 * Evento (nombre, fecha y precio) para que los Tickets queden ligados a las
 * condiciones de compra.
 */
public final class Reservation {

	private final UUID id;
	private final String userId;
	private final Long eventId;
	private final String eventName;
	private final LocalDateTime startsAt;
	private final BigDecimal price;
	private final ReservationStatus status;
	private final List<Ticket> tickets;
	private final LocalDateTime createdAt;

	private Reservation(UUID id, String userId, Long eventId, String eventName, LocalDateTime startsAt,
			BigDecimal price, ReservationStatus status, List<Ticket> tickets, LocalDateTime createdAt) {
		this.id = id;
		this.userId = userId;
		this.eventId = eventId;
		this.eventName = eventName;
		this.startsAt = startsAt;
		this.price = price;
		this.status = status;
		this.tickets = List.copyOf(tickets);
		this.createdAt = createdAt;
	}

	/**
	 * Crea una Reserva válida sobre un Evento: 1..N Tickets con N <= maxTickets,
	 * códigos únicos dentro de la Reserva, Usuario identificado y Evento no
	 * pasado. La Disponibilidad la controla {@link EventProjection#reserve}.
	 */
	public static Reservation book(UUID id, String userId, EventProjection event, List<Ticket> tickets,
			LocalDateTime now, int maxTickets) {
		if (id == null) {
			throw new IllegalArgumentException("id no puede ser null");
		}
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("userId no puede ser null o vacío");
		}
		if (event == null) {
			throw new IllegalArgumentException("event no puede ser null");
		}
		if (tickets == null || tickets.isEmpty()) {
			throw new IllegalArgumentException("una reserva necesita al menos 1 ticket");
		}
		if (tickets.size() > maxTickets) {
			throw new TooManyTicketsException(maxTickets);
		}
		if (!event.startsAt().isAfter(now)) {
			throw new EventNotBookableException(event.id(), event.startsAt());
		}
		long distinctCodes = tickets.stream().map(Ticket::code).distinct().count();
		if (distinctCodes != tickets.size()) {
			throw new IllegalArgumentException("los códigos de ticket deben ser únicos en la reserva");
		}
		return new Reservation(id, userId, event.id(), event.name(), event.startsAt(), event.price(),
			ReservationStatus.ACTIVE, tickets, now);
	}

	public UUID id() {
		return id;
	}

	public String userId() {
		return userId;
	}

	public Long eventId() {
		return eventId;
	}

	public String eventName() {
		return eventName;
	}

	public LocalDateTime startsAt() {
		return startsAt;
	}

	public BigDecimal price() {
		return price;
	}

	public ReservationStatus status() {
		return status;
	}

	public List<Ticket> tickets() {
		return tickets;
	}

	public LocalDateTime createdAt() {
		return createdAt;
	}
}
