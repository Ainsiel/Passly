package com.passly.catalog.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.passly.catalog.domain.EventCategory;

/**
 * Comando para crear o sustituir un Evento del catálogo (PUT full-replace).
 * La validación de forma (Bean Validation) ocurre en el borde web; el dominio
 * valida las invariantes al construir el {@code Event}.
 */
public record EventCommand(String name, String description, EventCategory category, String venue,
		LocalDateTime startsAt, BigDecimal price, int capacity) {
}
