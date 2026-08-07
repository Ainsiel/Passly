package com.passly.booking.adapter.in.web.dto;

import com.passly.booking.application.BookReservationCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo del POST /reservas. {@code email} es el destinatario al que se
 * entregan los Tickets (ticket #8). {@code maxTicketsPerReservation} no se
 * valida aquí para no duplicar la config: el tope lo aplica el dominio
 * (TooManyTicketsException -> 400).
 */
public record BookReservationRequest(@NotNull Long eventId, @NotNull @Min(1) Integer quantity,
		@NotBlank @Email String email) {

	public BookReservationCommand toCommand() {
		return new BookReservationCommand(eventId, quantity, email);
	}
}
