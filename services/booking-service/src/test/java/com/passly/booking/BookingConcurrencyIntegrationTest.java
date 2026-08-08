package com.passly.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.passly.booking.adapter.out.persistence.EventProjectionJpaEntity;
import com.passly.booking.adapter.out.persistence.EventProjectionJpaRepository;
import com.passly.booking.adapter.out.persistence.ReservationJpaRepository;
import com.passly.booking.support.AbstractMessagingIntegrationTest;
import com.passly.booking.support.StubJwtDecoderConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * Tests de concurrencia de la Reserva (AC del ticket #7): sobre un servidor
 * HTTP real (RANDOM_PORT), N usuarios compiten por los últimos Tickets y la
 * Disponibilidad nunca se sobregira, y N requests concurrentes con la misma
 * idempotency key crean una sola Reserva.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(StubJwtDecoderConfiguration.class)
class BookingConcurrencyIntegrationTest extends AbstractMessagingIntegrationTest {

	@LocalServerPort
	int port;

	@Autowired
	EventProjectionJpaRepository eventProjectionRepository;

	@Autowired
	ReservationJpaRepository reservationRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	org.springframework.transaction.support.TransactionTemplate transactionTemplate;

	private RestTemplate restTemplate;

	@BeforeEach
	void setUp() {
		restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
			@Override
			public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
				return false;
			}
		});
		jdbcTemplate.execute("TRUNCATE TABLE reservations CASCADE");
	}

	@Test
	void concurrentBookingsForTheLastTicketsReserveExactlyTheAvailability() throws Exception {
		transactionTemplate.executeWithoutResult(status -> eventProjectionRepository.upsert(7L, "Noche de Jazz",
			LocalDateTime.of(2026, 12, 31, 21, 0), new BigDecimal("30.00"), 10, 8));

		int users = 10;
		List<Integer> statuses = fireConcurrentBookings(users, i -> "user-" + i, i -> "key-" + i);

		long created = statuses.stream().filter(s -> s == HttpStatus.CREATED.value()).count();
		long conflicts = statuses.stream().filter(s -> s == HttpStatus.CONFLICT.value()).count();
		assertThat(statuses).hasSize(users);
		assertThat(created).isEqualTo(2);
		assertThat(conflicts).isEqualTo(users - created);

		EventProjectionJpaEntity projection = eventProjectionRepository.findById(7L).orElseThrow();
		assertThat(projection.getReservedTickets()).isEqualTo(10);
	}

	@Test
	void concurrentRequestsWithTheSameIdempotencyKeyCreateASingleReservation() throws Exception {
		transactionTemplate.executeWithoutResult(status -> eventProjectionRepository.upsert(7L, "Noche de Jazz",
			LocalDateTime.of(2026, 12, 31, 21, 0), new BigDecimal("30.00"), 100, 0));

		int requests = 10;
		List<Integer> statuses = fireConcurrentBookings(requests, i -> "user-1", i -> "key-1");

		long created = statuses.stream().filter(s -> s == HttpStatus.CREATED.value()).count();
		assertThat(statuses).hasSize(requests);
		assertThat(created).isEqualTo(1);
		assertThat(reservationRepository.findAll()).hasSize(1);
		EventProjectionJpaEntity projection = eventProjectionRepository.findById(7L).orElseThrow();
		assertThat(projection.getReservedTickets()).isEqualTo(1);
	}

	private List<Integer> fireConcurrentBookings(int n, java.util.function.Function<Integer, String> user,
			java.util.function.Function<Integer, String> idempotencyKey) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(n);
		CountDownLatch ready = new CountDownLatch(n);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Integer>> futures = new ArrayList<>();
		try {
			for (int i = 1; i <= n; i++) {
				final int index = i;
				futures.add(pool.submit(() -> {
					ready.countDown();
					start.await();
					return postBooking(user.apply(index), idempotencyKey.apply(index));
				}));
			}
			ready.await(30, TimeUnit.SECONDS);
			start.countDown();
			List<Integer> statuses = new ArrayList<>();
			for (Future<Integer> future : futures) {
				statuses.add(future.get(30, TimeUnit.SECONDS));
			}
			return statuses;
		}
		finally {
			pool.shutdownNow();
		}
	}

	private int postBooking(String user, String idempotencyKey) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(user);
		headers.set("X-Idempotency-Key", idempotencyKey);
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>("{\"eventId\":7,\"quantity\":1,\"email\":\"usuario@passly.local\"}",
			headers);
		ResponseEntity<String> response = restTemplate.postForEntity(url("/reservations"), entity, String.class);
		return response.getStatusCode().value();
	}

	private String url(String path) {
		return "http://localhost:" + port + path;
	}
}
