package com.passly.catalog.adapter.in.messaging;

/**
 * Topología de RabbitMQ del contrato {@code booking -> catalog} para
 * sincronizar la disponibilidad de eventos. Se duplica deliberadamente en
 * booking-service para no compartir clases entre servicios (ADR-0007).
 */
public final class RabbitTopology {

	public static final String BOOKINGS_EXCHANGE = "passly.bookings";
	public static final String ROUTING_KEY_RESERVATION_CREATED = "booking.reservation.created";
	public static final String CATALOG_AVAILABILITY_QUEUE = "catalog.reservation-created";
	public static final String TICKET_RESERVED_TYPE_ID = "passly:booking:ticket-reserved";

	private RabbitTopology() {
	}
}
