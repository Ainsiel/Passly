package com.passly.catalog.adapter.in.web.dto;

import java.util.List;

import com.passly.catalog.domain.Event;
import org.springframework.data.domain.Page;

/**
 * Página de resultados del listado con metadatos de paginación en el borde.
 */
public record EventPageResponse(List<EventSummaryResponse> content, int page, int size,
		long totalElements, int totalPages, boolean first, boolean last) {

	public static EventPageResponse from(Page<Event> page) {
		List<EventSummaryResponse> content = page.getContent().stream()
			.map(EventSummaryResponse::from)
			.toList();
		return new EventPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements(),
			page.getTotalPages(), page.isFirst(), page.isLast());
	}
}
