package com.passly.booking.adapter.in.messaging;

/**
 * Topología de RabbitMQ del contrato {@code catalog -> booking} (ADR-0011).
 * Duplicado deliberadamente del lado consumidor para no compartir clases
 * entre servicios.
 */
public final class RabbitTopology {

	public static final String EVENTS_EXCHANGE = "passly.events";
	public static final String ROUTING_KEY_CREATED = "catalog.event.created";
	public static final String ROUTING_KEY_UPDATED = "catalog.event.updated";
	public static final String EVENT_PROJECTIONS_QUEUE = "booking.event-projections";
	public static final String EVENT_ROUTING_PATTERN = "catalog.event.#";

	/**
	 * Id lógico del mensaje para el type mapping de Jackson. Cada servicio mapea
	 * este id a su propia copia del DTO, de modo que el FQCN no cruza servicios.
	 */
	public static final String EVENT_CHANGED_TYPE_ID = "passly:catalog:event-changed";

	private RabbitTopology() {
	}
}
