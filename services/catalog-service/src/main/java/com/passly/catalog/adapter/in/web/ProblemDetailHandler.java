package com.passly.catalog.adapter.in.web;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.passly.catalog.application.EventConflictException;
import com.passly.catalog.application.EventNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones del borde web a errores RFC 7807 (Problem Details,
 * content-type {@code application/problem+json}).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProblemDetailHandler {

	private static final URI EVENT_NOT_FOUND = URI.create("urn:problem-type:event-not-found");
	private static final URI EVENT_CONFLICT = URI.create("urn:problem-type:event-conflict");
	private static final URI BAD_REQUEST = URI.create("urn:problem-type:bad-request");
	private static final URI VALIDATION_ERROR = URI.create("urn:problem-type:validation-error");

	@ExceptionHandler(EventNotFoundException.class)
	ProblemDetail handleEventNotFound(EventNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Evento no encontrado");
		problem.setType(EVENT_NOT_FOUND);
		return problem;
	}

	@ExceptionHandler(EventConflictException.class)
	ProblemDetail handleEventConflict(EventConflictException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setTitle("Conflicto de evento");
		problem.setType(EVENT_CONFLICT);
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
			HttpMessageNotReadableException.class, PropertyReferenceException.class })
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
