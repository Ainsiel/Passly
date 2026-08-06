package com.passly.catalog.domain;

/**
 * Evento de dominio: un Evento del catálogo ha sido editado. Lleva la
 * instantánea completa actualizada del Evento; Reservas sustituye la
 * proyección por esta versión.
 */
public record EventUpdated(EventSnapshot snapshot) implements CatalogEvent {

	@Override
	public String type() {
		return "EventUpdated";
	}
}
