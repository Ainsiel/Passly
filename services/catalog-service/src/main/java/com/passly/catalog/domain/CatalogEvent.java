package com.passly.catalog.domain;

/**
 * Evento de dominio emitido cuando un Evento del catálogo cambia. Lo publica
 * el caso de uso de escritura a través del puerto {@code EventPublisher}.
 * {@code type} identifica la variante concreta en el contrato del wire.
 */
public sealed interface CatalogEvent permits EventCreated, EventUpdated {

	String type();

	EventSnapshot snapshot();
}
