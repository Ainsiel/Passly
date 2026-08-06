package com.passly.catalog.adapter.out.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.passly.catalog.domain.CatalogEvent;

/**
 * Forma del mensaje que viaja por RabbitMQ hacia el contexto Reservas.
 * {@code type} discrimina entre {@code EventCreated} y {@code EventUpdated} y
 * {@code event} es la instantánea completa del Evento. Es el contrato del wire
 * que Reservas refleja en su propia copia (ADR-0011).
 */
public record EventChangedMessage(String type, EventChangedMessage.EventData event) {

	public EventChangedMessage {
		if (type == null || event == null) {
			throw new IllegalArgumentException("type y event son obligatorios");
		}
	}

	public static EventChangedMessage from(CatalogEvent catalogEvent) {
		return new EventChangedMessage(catalogEvent.type(),
			new EventData(catalogEvent.snapshot().id(), catalogEvent.snapshot().name(),
				catalogEvent.snapshot().startsAt(), catalogEvent.snapshot().price(),
				catalogEvent.snapshot().capacity(), catalogEvent.snapshot().reservedTickets()));
	}

	public String routingKey() {
		return switch (type) {
			case "EventCreated" -> "catalog.event.created";
			case "EventUpdated" -> "catalog.event.updated";
			default -> throw new IllegalStateException("Tipo de evento de catálogo desconocido: " + type);
		};
	}

	public record EventData(Long id, String name, LocalDateTime startsAt, BigDecimal price, int capacity,
			int reservedTickets) {
	}
}
