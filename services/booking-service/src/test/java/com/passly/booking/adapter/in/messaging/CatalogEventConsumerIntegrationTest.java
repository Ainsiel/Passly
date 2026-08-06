package com.passly.booking.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.passly.booking.adapter.out.persistence.EventProjectionJpaEntity;
import com.passly.booking.adapter.out.persistence.EventProjectionJpaRepository;
import com.passly.booking.support.AbstractMessagingIntegrationTest;
import com.passly.booking.support.StubJwtDecoderConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Test D: el consumidor de eventos del catálogo mantiene la proyección de
 * reservas. Verifica convergencia: mensajes duplicados o reentregados no
 * corrompen el estado final, y los tipos desconocidos se descartan sin
 * romper el consumidor.
 */
@SpringBootTest
@Import(StubJwtDecoderConfiguration.class)
class CatalogEventConsumerIntegrationTest extends AbstractMessagingIntegrationTest {

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	EventProjectionJpaRepository repository;

	@Test
	void consumingEventCreatedUpsertsTheProjection() {
		publish("EventCreated", 7L, "Noche de Jazz", "2026-12-31T21:00:00", "30.00", 500, 120);

		EventProjectionJpaEntity projection = awaitProjection(7L);

		assertThat(projection.getName()).isEqualTo("Noche de Jazz");
		assertThat(projection.getStartsAt()).isEqualTo(LocalDateTime.of(2026, 12, 31, 21, 0));
		assertThat(projection.getPrice()).isEqualByComparingTo("30.00");
		assertThat(projection.getCapacity()).isEqualTo(500);
		assertThat(projection.getReservedTickets()).isEqualTo(120);
		assertThat(projection.getCapacity() - projection.getReservedTickets()).isEqualTo(380);
	}

	@Test
	void aDuplicatedDeliveryConvergesToTheSameFinalState() {
		publish("EventCreated", 7L, "Noche de Jazz", "2026-12-31T21:00:00", "30.00", 500, 120);
		publish("EventCreated", 7L, "Noche de Jazz", "2026-12-31T21:00:00", "30.00", 500, 120);

		EventProjectionJpaEntity projection = awaitProjection(7L);

		assertThat(projection.getName()).isEqualTo("Noche de Jazz");
		assertThat(projection.getCapacity()).isEqualTo(500);
		assertThat(projection.getReservedTickets()).isEqualTo(120);
		assertThat(repository.findAll().stream().filter(row -> row.getEventId().equals(7L))).hasSize(1);
	}

	@Test
	void consumingEventUpdatedAppliesTheNewSnapshot() {
		publish("EventCreated", 7L, "Noche de Jazz", "2026-12-31T21:00:00", "30.00", 500, 120);
		awaitProjection(7L);

		publish("EventUpdated", 7L, "Noche de Jazz (2027)", "2027-01-01T21:00:00", "35.00", 600, 120);

		EventProjectionJpaEntity projection = awaitProjectionName(7L, "Noche de Jazz (2027)");

		assertThat(projection.getStartsAt()).isEqualTo(LocalDateTime.of(2027, 1, 1, 21, 0));
		assertThat(projection.getPrice()).isEqualByComparingTo("35.00");
		assertThat(projection.getCapacity()).isEqualTo(600);
	}

	@Test
	void anUnknownEventTypeIsIgnored() {
		publish("EventArchived", 99L, "Fantasma", "2026-12-31T21:00:00", "10.00", 100, 0);

		awaitSettled();

		assertThat(repository.findById(99L)).isEmpty();
	}

	@Test
	void rawCatalogWireShapeIsConsumedAsAnEventCreated() {
		String rawBody = """
			{"type":"EventCreated","event":{"id":42,"name":"Noche de Jazz","startsAt":"2026-12-31T21:00:00",
			"price":30.00,"capacity":500,"reservedTickets":0}}
			""";
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		properties.setHeader("__TypeId__", RabbitTopology.EVENT_CHANGED_TYPE_ID);
		rabbitTemplate.send(RabbitTopology.EVENTS_EXCHANGE, RabbitTopology.ROUTING_KEY_CREATED,
			new Message(rawBody.getBytes(StandardCharsets.UTF_8), properties));

		EventProjectionJpaEntity projection = awaitProjection(42L);

		assertThat(projection.getName()).isEqualTo("Noche de Jazz");
		assertThat(projection.getCapacity()).isEqualTo(500);
		assertThat(projection.getReservedTickets()).isZero();
	}

	@Test
	void aMessageWithoutTypeIsAcknowledgedAndDiscarded() {
		publish(null, 100L, "Noche de Jazz", "2026-12-31T21:00:00", "30.00", 500, 0);

		awaitSettled();

		assertThat(repository.findById(100L)).isEmpty();
	}

	@Test
	void anInvalidSnapshotIsAcknowledgedAndDiscarded() {
		publish("EventCreated", 101L, "Noche de Jazz", "2026-12-31T21:00:00", "30.00", 500, 600);

		awaitSettled();

		assertThat(repository.findById(101L)).isEmpty();
	}

	private void publish(String type, long eventId, String name, String startsAt, String price, int capacity,
			int reservedTickets) {
		String routingKey = "EventCreated".equals(type)
			? RabbitTopology.ROUTING_KEY_CREATED
			: RabbitTopology.ROUTING_KEY_UPDATED;
		EventChangedMessage message = new EventChangedMessage(type, new EventChangedMessage.EventData(eventId, name,
			LocalDateTime.parse(startsAt), new BigDecimal(price), capacity, reservedTickets));
		rabbitTemplate.convertAndSend(RabbitTopology.EVENTS_EXCHANGE, routingKey, message);
	}

	private EventProjectionJpaEntity awaitProjection(long eventId) {
		return awaitProjection(eventId, null);
	}

	private EventProjectionJpaEntity awaitProjectionName(long eventId, String name) {
		return awaitProjection(eventId, name);
	}

	private EventProjectionJpaEntity awaitProjection(long eventId, String expectedName) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (System.nanoTime() < deadline) {
			Optional<EventProjectionJpaEntity> found = repository.findById(eventId);
			if (found.isPresent() && (expectedName == null || expectedName.equals(found.get().getName()))) {
				return found.get();
			}
			sleepQuietly(50);
		}
		throw new AssertionError("No se recibió la proyección del evento " + eventId + " a tiempo");
	}

	private void awaitSettled() {
		try {
			Thread.sleep(500);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
