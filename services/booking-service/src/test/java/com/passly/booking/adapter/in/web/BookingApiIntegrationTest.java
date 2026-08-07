package com.passly.booking.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.passly.booking.adapter.out.persistence.EventProjectionJpaEntity;
import com.passly.booking.adapter.out.persistence.EventProjectionJpaRepository;
import com.passly.booking.support.AbstractMessagingIntegrationTest;
import com.passly.booking.support.StubJwtDecoderConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test del borde web de Reservas sobre Postgres/RabbitMQ reales. El JWT se
 * sustituye por un stub (mismo patrón que el EventApiIntegrationTest del
 * catálogo): los sujetos {@code user-1} y {@code user-2} son los Usuarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(StubJwtDecoderConfiguration.class)
class BookingApiIntegrationTest extends AbstractMessagingIntegrationTest {

	private static final String EVENT_BODY = """
		{"eventId":7,"quantity":%d,"email":"usuario@passly.local"}
		""";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	EventProjectionJpaRepository eventProjectionRepository;

	@Autowired
	org.springframework.transaction.support.TransactionTemplate transactionTemplate;

	@Autowired
	org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanAndSeedEvent() {
		jdbcTemplate.execute("TRUNCATE TABLE reservations CASCADE");
		transactionTemplate.executeWithoutResult(status -> eventProjectionRepository.upsert(7L, "Noche de Jazz",
			LocalDateTime.of(2026, 12, 31, 21, 0), new BigDecimal("30.00"), 500, 0));
	}

	@Test
	void bookingDecrementsAvailabilityAndReturnsTicketsWithCodeAndQr() throws Exception {
		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_BODY.formatted(2)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.eventId").value(7))
			.andExpect(jsonPath("$.eventName").value("Noche de Jazz"))
			.andExpect(jsonPath("$.price").value(30.0))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.tickets", hasSize(2)))
			.andExpect(jsonPath("$.tickets[*].code", everyItem(not(emptyString()))))
			.andExpect(jsonPath("$.tickets[*].qr", everyItem(not(emptyString()))));

		EventProjectionJpaEntity projection = eventProjectionRepository.findById(7L).orElseThrow();
		assertThat(projection.getReservedTickets()).isEqualTo(2);
	}

	@Test
	void replayingTheSameIdempotencyKeyReturnsTheSameReservation() throws Exception {
		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_BODY.formatted(1)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tickets", hasSize(1)));

		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_BODY.formatted(1)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tickets", hasSize(1)));

		mockMvc.perform(get("/reservas").header("Authorization", "Bearer user-1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)));

		EventProjectionJpaEntity projection = eventProjectionRepository.findById(7L).orElseThrow();
		assertThat(projection.getReservedTickets()).isEqualTo(1);
	}

	@Test
	void aUserCannotHoldTwoActiveReservationsForTheSameEvent() throws Exception {
		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_BODY.formatted(1)))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-2")
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_BODY.formatted(1)))
			.andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("urn:problem-type:duplicate-reservation"))
			.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void bookingASoldOutEventReturns409() throws Exception {
		transactionTemplate.executeWithoutResult(status -> eventProjectionRepository.upsert(8L, "Agotado",
			LocalDateTime.of(2026, 12, 31, 21, 0), new BigDecimal("30.00"), 100, 100));

		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventId\":8,\"quantity\":1,\"email\":\"usuario@passly.local\"}"))
			.andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("urn:problem-type:sold-out"))
			.andExpect(jsonPath("$.detail").value(containsString("agotado")));
	}

	@Test
	void bookingMoreThanTheMaxTicketsReturns400() throws Exception {
		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_BODY.formatted(5)))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("urn:problem-type:too-many-tickets"))
			.andExpect(jsonPath("$.detail").value(containsString("4")));
	}

	@Test
	void bookingANonexistentEventReturns404() throws Exception {
		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventId\":999,\"quantity\":1,\"email\":\"usuario@passly.local\"}"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("urn:problem-type:event-not-found"))
			.andExpect(jsonPath("$.detail").value("No existe un evento con id 999"));
	}

	@Test
	void bookingAnEventThatAlreadyStartedReturns409() throws Exception {
		transactionTemplate.executeWithoutResult(status -> eventProjectionRepository.upsert(9L, "Ya empezó",
			LocalDateTime.of(2020, 1, 1, 21, 0), new BigDecimal("30.00"), 100, 0));

		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventId\":9,\"quantity\":1,\"email\":\"usuario@passly.local\"}"))
			.andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("urn:problem-type:event-not-bookable"));
	}

	@Test
	void anInvalidQuantityReturns400() throws Exception {
		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventId\":7,\"quantity\":0,\"email\":\"usuario@passly.local\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("urn:problem-type:validation-error"))
			.andExpect(jsonPath("$.errors[0].field").value("quantity"));
	}

	@Test
	void aMissingIdempotencyKeyReturns400() throws Exception {
		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_BODY.formatted(1)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void myReservationsReturnsOnlyTheUsersReservations() throws Exception {
		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_BODY.formatted(1)))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-2")
				.header("X-Idempotency-Key", "key-2")
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_BODY.formatted(1)))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/reservas").header("Authorization", "Bearer user-1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].tickets[0].code").isNotEmpty())
			.andExpect(jsonPath("$[0].tickets[0].qr").isNotEmpty());

		mockMvc.perform(get("/reservas").header("Authorization", "Bearer user-2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void anonymousRequestIsRejectedWith401() throws Exception {
		mockMvc.perform(post("/reservas")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(EVENT_BODY.formatted(1)))
			.andExpect(status().isUnauthorized());
	}
}
