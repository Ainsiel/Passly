package com.passly.booking.adapter.in.web.dto;

import com.passly.booking.application.BookReservationCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Body of POST /reservations. {@code email} is the recipient to whom Tickets
 * are delivered (ticket #8). {@code maxTicketsPerReservation} is not validated
 * here to avoid duplicating config: the cap is enforced by the domain
 * (TooManyTicketsException -> 400).
 */
public record BookReservationRequest(@NotNull Long eventId, @NotNull @Min(1) Integer quantity,
		@NotBlank @Email String email) {

	public BookReservationCommand toCommand() {
		return new BookReservationCommand(eventId, quantity, email);
	}
}
