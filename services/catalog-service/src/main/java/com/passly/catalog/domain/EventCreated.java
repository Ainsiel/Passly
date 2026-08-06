package com.passly.catalog.domain;

/**
 * Evento de dominio: un Evento ha sido creado en el catálogo. Lleva la
 * instantánea completa del Evento para que Reservas pueda construir su
 * proyección.
 */
public record EventCreated(EventSnapshot snapshot) implements CatalogEvent {

	@Override
	public String type() {
		return "EventCreated";
	}
}
