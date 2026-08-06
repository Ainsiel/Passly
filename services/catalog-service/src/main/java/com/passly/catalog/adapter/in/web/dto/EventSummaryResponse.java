package com.passly.catalog.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.passly.catalog.domain.Event;
import com.passly.catalog.domain.EventCategory;

/**
 * Representación compacta de un Evento para el listado paginado.
 */
public record EventSummaryResponse(Long id, String name, EventCategory category, String venue,
		LocalDateTime startsAt, BigDecimal price, int available) {

	public static EventSummaryResponse from(Event event) {
		return new EventSummaryResponse(event.id(), event.name(), event.category(), event.venue(),
			event.startsAt(), event.price(), event.available());
	}
}
