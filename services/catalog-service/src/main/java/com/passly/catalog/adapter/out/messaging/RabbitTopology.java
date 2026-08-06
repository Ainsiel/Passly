package com.passly.catalog.adapter.out.messaging;

/**
 * Topología de mensajería del contrato Catálogo → Reservas (ADR-0011).
 * El exchange y las routing keys se definen aquí y en el consumidor de
 * Reservas de forma idéntica; no hay librería compartida entre servicios
 * (ADR-0007), por lo que este contrato se mantiene a mano sincronizado.
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
