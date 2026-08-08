package com.passly.booking.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class ProblemDetailHandlerTest {

	private final ProblemDetailHandler handler = new ProblemDetailHandler();

	@Test
	void optimisticLockingConflictMapsTo409() {
		ProblemDetail problem = handler
			.handleOptimisticLockingConflict(new ObjectOptimisticLockingFailureException("EventProjection", 1L));

		assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
		assertThat(problem.getTitle()).isEqualTo("Concurrency conflict");
		assertThat(problem.getType().toString()).isEqualTo("urn:problem-type:concurrency-conflict");
	}
}
