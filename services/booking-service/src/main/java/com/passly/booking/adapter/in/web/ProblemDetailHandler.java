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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates Reservations context exceptions to RFC 7807 errors (Problem
 * Details, content-type {@code application/problem+json}), same style as the
 * catalog.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProblemDetailHandler {

	private static final URI EVENT_NOT_FOUND = URI.create("urn:problem-type:event-not-found");
	private static final URI DUPLICATE_RESERVATION = URI.create("urn:problem-type:duplicate-reservation");
	private static final URI SOLD_OUT = URI.create("urn:problem-type:sold-out");
	private static final URI EVENT_NOT_BOOKABLE = URI.create("urn:problem-type:event-not-bookable");
	private static final URI TOO_MANY_TICKETS = URI.create("urn:problem-type:too-many-tickets");
	private static final URI CONCURRENCY_CONFLICT = URI.create("urn:problem-type:concurrency-conflict");
	private static final URI BAD_REQUEST = URI.create("urn:problem-type:bad-request");
	private static final URI VALIDATION_ERROR = URI.create("urn:problem-type:validation-error");

	@ExceptionHandler(EventNotFoundException.class)
	ProblemDetail handleEventNotFound(EventNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Event not found");
		problem.setType(EVENT_NOT_FOUND);
		return problem;
	}

	@ExceptionHandler(DuplicateReservationException.class)
	ProblemDetail handleDuplicateReservation(DuplicateReservationException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Duplicate reservation");
		problem.setType(DUPLICATE_RESERVATION);
		return problem;
	}

	@ExceptionHandler(SoldOutException.class)
	ProblemDetail handleSoldOut(SoldOutException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Sold out");
		problem.setType(SOLD_OUT);
		return problem;
	}

	@ExceptionHandler(EventNotBookableException.class)
	ProblemDetail handleEventNotBookable(EventNotBookableException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Event not bookable");
		problem.setType(EVENT_NOT_BOOKABLE);
		return problem;
	}

	@ExceptionHandler(TooManyTicketsException.class)
	ProblemDetail handleTooManyTickets(TooManyTicketsException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Too many tickets");
		problem.setType(TOO_MANY_TICKETS);
		return problem;
	}

	/**
	 * Optimistic locking conflict that exhausted use-case retries: two concurrent
	 * Reservations competed for the last Tickets and this one lost. Although the
	 * internal retry normally resolves it with 409 sold-out, this is the safety
	 * net that guarantees a 500 is never returned: losers always get a 4xx (AC
	 * of ticket #7).
	 */
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLockingConflict(ObjectOptimisticLockingFailureException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
			"Concurrency conflict: retry the reservation");
		problem.setTitle("Concurrency conflict");
		problem.setType(CONCURRENCY_CONFLICT);
		return problem;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
		List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
			.map(ProblemDetailHandler::toError)
			.toList();
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			errors.size() + " validation error(s)");
		problem.setTitle("Invalid request");
		problem.setType(VALIDATION_ERROR);
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
			HttpMessageNotReadableException.class })
	ProblemDetail handleBadRequest(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Invalid request");
		problem.setType(BAD_REQUEST);
		return problem;
	}

	private static Map<String, String> toError(FieldError error) {
		return Map.of("field", error.getField(), "message", error.getDefaultMessage());
	}
}
