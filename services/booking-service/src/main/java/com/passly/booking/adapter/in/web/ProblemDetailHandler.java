package com.passly.booking.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.passly.booking.application.DuplicateReservationException;
import com.passly.booking.application.EventNotFoundException;
import com.passly.booking.domain.EventNotBookableException;
import com.passly.booking.domain.SoldOutException;
import com.passly.booking.domain.TooManyTicketsException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones del contexto Reservas a errores RFC 7807 (Problem
 * Details, content-type {@code application/problem+json}), mismo estilo que el
 * catálogo.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProblemDetailHandler {

	private static final URI EVENT_NOT_FOUND = URI.create("urn:problem-type:event-not-found");
	private static final URI DUPLICATE_RESERVATION = URI.create("urn:problem-type:duplicate-reservation");
	private static final URI SOLD_OUT = URI.create("urn:problem-type:sold-out");
	private static final URI EVENT_NOT_BOOKABLE = URI.create("urn:problem-type:event-not-bookable");
	private static final URI TOO_MANY_TICKETS = URI.create("urn:problem-type:too-many-tickets");
	private static final URI BAD_REQUEST = URI.create("urn:problem-type:bad-request");
	private static final URI VALIDATION_ERROR = URI.create("urn:problem-type:validation-error");

	@ExceptionHandler(EventNotFoundException.class)
	ProblemDetail handleEventNotFound(EventNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Evento no encontrado");
		problem.setType(EVENT_NOT_FOUND);
		return problem;
	}

	@ExceptionHandler(DuplicateReservationException.class)
	ProblemDetail handleDuplicateReservation(DuplicateReservationException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Reserva duplicada");
		problem.setType(DUPLICATE_RESERVATION);
		return problem;
	}

	@ExceptionHandler(SoldOutException.class)
	ProblemDetail handleSoldOut(SoldOutException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Sin disponibilidad");
		problem.setType(SOLD_OUT);
		return problem;
	}

	@ExceptionHandler(EventNotBookableException.class)
	ProblemDetail handleEventNotBookable(EventNotBookableException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Evento no reservable");
		problem.setType(EVENT_NOT_BOOKABLE);
		return problem;
	}

	@ExceptionHandler(TooManyTicketsException.class)
	ProblemDetail handleTooManyTickets(TooManyTicketsException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Demasiados tickets");
		problem.setType(TOO_MANY_TICKETS);
		return problem;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
		List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
			.map(ProblemDetailHandler::toError)
			.toList();
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			errors.size() + " error(es) de validación");
		problem.setTitle("Solicitud inválida");
		problem.setType(VALIDATION_ERROR);
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
			HttpMessageNotReadableException.class })
	ProblemDetail handleBadRequest(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Solicitud inválida");
		problem.setType(BAD_REQUEST);
		return problem;
	}

	private static Map<String, String> toError(FieldError error) {
		return Map.of("field", error.getField(), "message", error.getDefaultMessage());
	}
}
