package com.passly.booking.adapter.out.codegen;

import java.util.Locale;
import java.util.UUID;

import com.passly.booking.application.port.TicketCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * Genera códigos de Ticket únicos: 16 caracteres hex aleatorios derivados de
 * un UUID. La unicidad global la garantiza la columna {@code code} (UNIQUE);
 * el prefijo es solo legibilidad.
 */
@Component
public class UuidTicketCodeGenerator implements TicketCodeGenerator {

	@Override
	public String next() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
	}
}
