package com.passly.catalog.adapter.in.web;

import java.net.URI;

import com.passly.catalog.application.EventNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
	private static final URI BAD_REQUEST = URI.create("urn:problem-type:bad-request");

	@ExceptionHandler(EventNotFoundException.class)
	ProblemDetail handleEventNotFound(EventNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setTitle("Evento no encontrado");
		problem.setType(EVENT_NOT_FOUND);
		return problem;
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
			PropertyReferenceException.class })
	ProblemDetail handleBadRequest(Exception ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Solicitud inválida");
		problem.setType(BAD_REQUEST);
		return problem;
	}
}
