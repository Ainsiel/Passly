package com.passly.booking.adapter.in.web;

import java.util.List;

import com.passly.booking.adapter.in.web.dto.BookReservationRequest;
import com.passly.booking.adapter.in.web.dto.ReservationResponse;
import com.passly.booking.application.BookingResult;
import com.passly.booking.application.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Borde web del contexto Reservas. El Usuario sale del subject del JWT; la
 * idempotency key viaja en {@code X-Idempotency-Key} para que el cliente pueda
 * reintentar sin duplicar. Un reenvío de la misma key devuelve 200 con la misma
 * Reserva; una creación nueva devuelve 201.
 */
@Validated
@RestController
@RequestMapping("/reservas")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping
	public ResponseEntity<ReservationResponse> book(
			@RequestHeader("X-Idempotency-Key") @NotBlank String idempotencyKey,
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody BookReservationRequest request) {
		BookingResult result = reservationService.book(jwt.getSubject(), idempotencyKey, request.toCommand());
		ReservationResponse body = ReservationResponse.from(result.reservation());
		return result.replayed()
			? ResponseEntity.ok(body)
			: ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping
	public List<ReservationResponse> myReservations(@AuthenticationPrincipal Jwt jwt) {
		return reservationService.myReservations(jwt.getSubject()).stream()
			.map(ReservationResponse::from)
			.toList();
	}
}
