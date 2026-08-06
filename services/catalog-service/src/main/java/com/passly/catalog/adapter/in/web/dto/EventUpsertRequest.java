package com.passly.catalog.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.passly.catalog.application.EventCommand;
import com.passly.catalog.domain.EventCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /events y PUT /events/{id} (sustitución completa). Valida la
 * forma de la entrada en el borde web, alineada con el schema de la tabla
 * {@code events}. {@code reservedTickets} no es editable: lo gestiona el
 * contexto Reservas.
 */
public record EventUpsertRequest(
		@NotBlank @Size(max = 150) String name,
		String description,
		@NotNull EventCategory category,
		@NotBlank @Size(max = 150) String venue,
		@NotNull LocalDateTime startsAt,
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
		@NotNull @Min(0) Integer capacity) {

	public EventCommand toCommand() {
		return new EventCommand(name, description, category, venue, startsAt, price, capacity);
	}
}
