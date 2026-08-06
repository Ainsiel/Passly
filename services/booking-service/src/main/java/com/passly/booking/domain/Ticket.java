package com.passly.booking.domain;

/**
 * Entidad Ticket emitida dentro de una {@link Reservation}. Tiene un código
 * único y el payload de su código QR: el string que el QR codificaría (una URL
 * canónica del ticket), renderizable después en el email o en la web. El QR
 * como imagen no se genera aquí (ADR / ticket #7): se persiste el payload.
 */
public record Ticket(String code, String qr) {

	public Ticket {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("code no puede ser null o vacío");
		}
		if (qr == null || qr.isBlank()) {
			throw new IllegalArgumentException("qr no puede ser null o vacío");
		}
	}
}
