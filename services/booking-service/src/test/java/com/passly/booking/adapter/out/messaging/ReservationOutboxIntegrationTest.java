package com.passly.booking.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.passly.booking.adapter.out.persistence.EventProjectionJpaRepository;
import com.passly.booking.adapter.out.persistence.ReservationOutboxJpaEntity;
import com.passly.booking.adapter.out.persistence.ReservationOutboxJpaRepository;
import com.passly.booking.support.AbstractMessagingIntegrationTest;
import com.passly.booking.support.StubJwtDecoderConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Test del outbox de Reservas (ticket #8, AC1): la Reserva escribe su fila de
 * outbox en la misma transacción, el poller la publica a RabbitMQ y la marca
 * publicada, y una publicación que falla deja la fila pendiente para el
 * siguiente ciclo. El poller se desactiva (fixed-delay 1h) para que el ciclo
 * sea determinista.
 */
@SpringBootTest(properties = { "passly.outbox.poller.fixed-delay=3600000" })
@AutoConfigureMockMvc
@Import(StubJwtDecoderConfiguration.class)
class ReservationOutboxIntegrationTest extends AbstractMessagingIntegrationTest {

	private static final String BOOK_BODY = "{\"eventId\":7,\"quantity\":%d,\"email\":\"usuario@passly.local\"}";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	AmqpAdmin amqpAdmin;

	@Autowired
	ReservationOutboxPoller outboxPoller;

	@Autowired
	ReservationOutboxJpaRepository outboxRepository;

	@Autowired
	EventProjectionJpaRepository eventProjectionRepository;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	org.springframework.transaction.support.TransactionTemplate transactionTemplate;

	@Autowired
	org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

	private Queue testQueue;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("TRUNCATE TABLE reservations CASCADE");
		jdbcTemplate.execute("TRUNCATE TABLE reservation_outbox RESTART IDENTITY CASCADE");
		transactionTemplate.executeWithoutResult(status -> eventProjectionRepository.upsert(7L, "Noche de Jazz",
			LocalDateTime.of(2026, 12, 31, 21, 0), new BigDecimal("30.00"), 500, 0));
		testQueue = new Queue("test." + System.nanoTime(), true, false, false);
		amqpAdmin.declareQueue(testQueue);
		amqpAdmin.declareBinding(BindingBuilder
			.bind(testQueue)
			.to(new TopicExchange(ReservationTopology.BOOKINGS_EXCHANGE))
			.with(ReservationTopology.ROUTING_KEY_RESERVATION_CREATED));
	}

	@Test
	void bookingWritesAnOutboxRowInTheSameTransaction() throws Exception {
		book("user-1", "key-1", 2);

		List<ReservationOutboxJpaEntity> rows = outboxRepository.findAll();
		assertThat(rows).hasSize(1);
		ReservationOutboxJpaEntity row = rows.get(0);
		assertThat(row.getPublishedAt()).isNull();
		JsonNode payload = objectMapper.readTree(row.getPayload());
		assertThat(payload.get("email").asText()).isEqualTo("usuario@passly.local");
		assertThat(payload.get("eventName").asText()).isEqualTo("Noche de Jazz");
		assertThat(payload.get("tickets")).hasSize(2);
	}

	@Test
	void aReplayDoesNotWriteASecondOutboxRow() throws Exception {
		book("user-1", "key-1", 1);
		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content(BOOK_BODY.formatted(1)))
			.andExpect(status().isOk());

		assertThat(outboxRepository.findAll()).hasSize(1);
	}

	@Test
	void aSoldOutBookingLeavesNoOutboxRow() throws Exception {
		transactionTemplate.executeWithoutResult(status -> eventProjectionRepository.upsert(8L, "Agotado",
			LocalDateTime.of(2026, 12, 31, 21, 0), new BigDecimal("30.00"), 100, 100));

		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer user-1")
				.header("X-Idempotency-Key", "key-1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"eventId\":8,\"quantity\":1,\"email\":\"usuario@passly.local\"}"))
			.andExpect(status().isConflict());

		assertThat(outboxRepository.findAll()).isEmpty();
	}

	@Test
	void thePollerPublishesTheReservationToRabbitMqAndMarksItPublished() throws Exception {
		book("user-1", "key-1", 1);

		outboxPoller.publishPending();

		JsonNode message = receiveMessage();
		assertThat(message.get("email").asText()).isEqualTo("usuario@passly.local");
		assertThat(message.get("eventName").asText()).isEqualTo("Noche de Jazz");
		assertThat(message.at("/tickets/0/code").asText()).isNotEmpty();
		assertThat(message.at("/tickets/0/qr").asText()).startsWith("https://passly.local/tickets/");
		assertOutboxRowPublished();

		outboxPoller.publishPending();
		assertThat(rabbitTemplate.receive(testQueue.getName(), TimeUnit.SECONDS.toMillis(2))).isNull();
	}

	@Test
	void theMessageCarriesTheLogicalTypeIdOfTheContract() throws Exception {
		book("user-1", "key-1", 1);

		outboxPoller.publishPending();

		org.springframework.amqp.core.Message message = rabbitTemplate.receive(testQueue.getName(),
			TimeUnit.SECONDS.toMillis(5));
		assertThat(message).isNotNull();
		assertThat(message.getMessageProperties().getHeaders().get("__TypeId__"))
			.isEqualTo(ReservationTopology.TICKET_RESERVED_TYPE_ID);
	}

	@Test
	void thePublishedPayloadSatisfiesTheNotificationContract() throws Exception {
		book("user-1", "key-1", 2);

		outboxPoller.publishPending();

		JsonNode payload = receiveMessage();
		ContractMessage message = objectMapper.treeToValue(payload, ContractMessage.class);
		assertThat(message).isNotNull();
		assertThat(message.reservationId()).isNotNull();
		assertThat(message.email()).isEqualTo("usuario@passly.local");
		assertThat(message.eventName()).isEqualTo("Noche de Jazz");
		assertThat(message.startsAt()).isNotNull();
		assertThat(message.price()).isPositive();
		assertThat(message.tickets()).hasSize(2);
		assertThat(message.tickets()).allSatisfy(ticket -> {
			assertThat(ticket.code()).isNotBlank();
			assertThat(ticket.qr()).isNotBlank();
		});
	}

	@Test
	void aFailedPublishLeavesTheRowPendingAndTheNextCycleRepublishesIt() throws Exception {
		book("user-1", "key-1", 1);
		ReservationOutboxJpaEntity row = outboxRepository.findAll().get(0);
		String originalPayload = row.getPayload();
		row.setPayload("{\"reservationId\":\"11111111-1111-1111-1111-111111111111\"}");
		outboxRepository.save(row);

		outboxPoller.publishPending();

		assertThat(outboxRepository.findAll().get(0).getPublishedAt()).isNull();
		assertThat(rabbitTemplate.receive(testQueue.getName(), TimeUnit.SECONDS.toMillis(2))).isNull();

		ReservationOutboxJpaEntity repaired = outboxRepository.findAll().get(0);
		repaired.setPayload(originalPayload);
		outboxRepository.save(repaired);

		outboxPoller.publishPending();

		assertThat(receiveMessage()).isNotNull();
		assertOutboxRowPublished();
	}

	private void book(String user, String idempotencyKey, int quantity) throws Exception {
		mockMvc.perform(post("/reservas")
				.header("Authorization", "Bearer " + user)
				.header("X-Idempotency-Key", idempotencyKey)
				.contentType(MediaType.APPLICATION_JSON)
				.content(BOOK_BODY.formatted(quantity)))
			.andExpect(status().isCreated());
	}

	private JsonNode receiveMessage() throws Exception {
		org.springframework.amqp.core.Message message = rabbitTemplate.receive(testQueue.getName(),
			TimeUnit.SECONDS.toMillis(5));
		assertThat(message).isNotNull();
		return objectMapper.readTree(message.getBody());
	}

	private void assertOutboxRowPublished() {
		assertThat(outboxRepository.findAll())
			.singleElement()
			.satisfies(row -> assertThat(row.getPublishedAt()).isNotNull());
	}

	/**
	 * Espejo del DTO estricto de notification-service (ADR-0007): valida que el
	 * payload emitido por booking es consumible por el listener de
	 * notification, cuyo constructor rechaza campos obligatorios ausentes.
	 */
	private record ContractMessage(java.util.UUID reservationId, String email, String eventName,
			LocalDateTime startsAt, BigDecimal price, List<ContractTicket> tickets) {

		private ContractMessage {
			if (reservationId == null || email == null || email.isBlank() || eventName == null || startsAt == null
					|| price == null || tickets == null || tickets.isEmpty()) {
				throw new IllegalArgumentException("contrato del ticket-reserved violado");
			}
		}

		private record ContractTicket(String code, String qr) {
		}
	}
}
