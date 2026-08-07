package com.passly.notification.adapter.in.messaging;

/**
 * Topología de RabbitMQ del contrato {@code booking -> notification} (ticket
 * #8). Se duplica deliberadamente en notification-service para no compartir
 * clases entre servicios (ADR-0007): los valores de exchange, routing key,
 * cola y type-id son el contrato del wire.
 *
 * <p>La cola de trabajo es durable y declara una dead-letter exchange
 * ({@code passly.bookings.dlx}) con su cola ({@code notification.ticket-reserved.dlq}):
 * los mensajes intratables que agotan el reintento se descartan allí en vez de
 * reintentarse en bucle (AC3).
 */
public final class RabbitTopology {

	public static final String BOOKINGS_EXCHANGE = "passly.bookings";
	public static final String ROUTING_KEY_RESERVATION_CREATED = "booking.reservation.created";
	public static final String TICKET_RESERVED_QUEUE = "notification.ticket-reserved";
	public static final String TICKET_RESERVED_DLX = "passly.bookings.dlx";
	public static final String TICKET_RESERVED_DLQ = "notification.ticket-reserved.dlq";

	/**
	 * Id lógico del mensaje para el type mapping de Jackson. Cada servicio mapea
	 * este id a su propia copia del DTO, de modo que el FQCN no cruza servicios.
	 */
	public static final String TICKET_RESERVED_TYPE_ID = "passly:booking:ticket-reserved";

	private RabbitTopology() {
	}
}
