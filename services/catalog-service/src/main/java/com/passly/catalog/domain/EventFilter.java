package com.passly.catalog.domain;

import java.time.LocalDate;

/**
 * Criterios opcionales para filtrar la lista de Eventos. Los componentes null
 * o en blanco significan "sin filtro".
 */
public record EventFilter(String text, EventCategory category, LocalDate date, String venue) {

	public boolean hasText() {
		return text != null && !text.isBlank();
	}

	public boolean hasCategory() {
		return category != null;
	}

	public boolean hasDate() {
		return date != null;
	}

	public boolean hasVenue() {
		return venue != null && !venue.isBlank();
	}
}
