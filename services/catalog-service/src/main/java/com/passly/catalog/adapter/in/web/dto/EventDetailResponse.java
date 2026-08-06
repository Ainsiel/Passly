package com.passly.catalog.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.passly.catalog.domain.Event;
import com.passly.catalog.domain.EventCategory;

/**
 * Detalle completo de un Evento: incluye fecha, lugar, categoría, precio,
 * Capacidad y Disponibilidad (derivada).
 */
public record EventDetailResponse(Long id, String name, String description, EventCategory category,
		String venue, LocalDateTime startsAt, BigDecimal price, int capacity, int available) {

	public static EventDetailResponse from(Event event) {
		return new EventDetailResponse(event.id(), event.name(), event.description(), event.category(),
			event.venue(), event.startsAt(), event.price(), event.capacity(), event.available());
	}
}
