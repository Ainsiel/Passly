package com.passly.catalog.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.passly.catalog.adapter.out.persistence.EventOutboxJpaRepository;
import com.passly.catalog.support.AbstractMessagingIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = { "passly.outbox.poller.fixed-delay=3600000" })
@AutoConfigureMockMvc
@Import(EventPublishIntegrationTest.JwtDecoderStubConfiguration.class)
class EventPublishIntegrationTest extends AbstractMessagingIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	AmqpAdmin amqpAdmin;

	@Autowired
	EventOutboxPoller eventOutboxPoller;

	@Autowired
	EventOutboxJpaRepository outboxRepository;

	@Autowired
	ObjectMapper objectMapper;

	private Queue testQueue;

	@BeforeEach
	void bindTestQueueToEventsExchange() {
		testQueue = new Queue("test." + System.nanoTime(), true, false, false);
		amqpAdmin.declareQueue(testQueue);
		amqpAdmin.declareBinding(BindingBuilder
			.bind(testQueue)
			.to(new TopicExchange(RabbitTopology.EVENTS_EXCHANGE))
			.with(RabbitTopology.EVENT_ROUTING_PATTERN));
	}

	@Test
	void creatingAnEventViaApiPublishesEventCreatedToRabbitMq() throws Exception {
		long createdId = createEvent("""
			{"name":"Noche de Jazz a la Fresca","description":"Velada de jazz íntima",
			"category":"CONCIERTO","venue":"Auditorio","startsAt":"2026-12-31T21:00:00",
			"price":30.00,"capacity":500}
			""");

		eventOutboxPoller.publishPending();

		JsonNode message = receiveMessage();
		assertThat(message.get("type").asText()).isEqualTo("EventCreated");
		assertThat(message.at("/event/id").asLong()).isEqualTo(createdId);
		assertThat(message.at("/event/name").asText()).isEqualTo("Noche de Jazz a la Fresca");
		assertThat(message.at("/event/startsAt").asText()).isEqualTo("2026-12-31T21:00:00");
		assertThat(message.at("/event/price").decimalValue()).isEqualByComparingTo("30.00");
		assertThat(message.at("/event/capacity").asInt()).isEqualTo(500);
		assertThat(message.at("/event/reservedTickets").asInt()).isZero();
		assertOutboxRowMarkedPublished(createdId);
	}

	@Test
	void editingAnEventViaApiPublishesEventUpdatedWithTheNewSnapshot() throws Exception {
		long createdId = createEvent("""
			{"name":"Noche de Jazz a la Fresca","description":"Velada de jazz íntima",
			"category":"CONCIERTO","venue":"Auditorio","startsAt":"2026-12-31T21:00:00",
			"price":30.00,"capacity":500}
			""");
		eventOutboxPoller.publishPending();
		receiveMessage();

		mockMvc.perform(put("/events/" + createdId)
				.header("Authorization", "Bearer admin")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"Noche de Jazz a la Fresca (2027)","description":"Velada de jazz íntima",
					"category":"CONCIERTO","venue":"Auditorio Nacional","startsAt":"2027-01-01T21:00:00",
					"price":35.00,"capacity":600}
					"""))
			.andExpect(status().isOk());

		eventOutboxPoller.publishPending();

		JsonNode message = receiveMessage();
		assertThat(message.get("type").asText()).isEqualTo("EventUpdated");
		assertThat(message.at("/event/id").asLong()).isEqualTo(createdId);
		assertThat(message.at("/event/capacity").asInt()).isEqualTo(600);
		assertThat(message.at("/event/reservedTickets").asInt()).isZero();
	}

	@Test
	void pollingTwiceDoesNotRepublishAlreadySentEvents() throws Exception {
		createEvent("""
			{"name":"Noche de Jazz a la Fresca","description":"Velada de jazz íntima",
			"category":"CONCIERTO","venue":"Auditorio","startsAt":"2026-12-31T21:00:00",
			"price":30.00,"capacity":500}
			""");

		eventOutboxPoller.publishPending();
		assertThat(receiveMessage()).isNotNull();

		eventOutboxPoller.publishPending();

		assertThat(rabbitTemplate.receive(testQueue.getName(), TimeUnit.SECONDS.toMillis(2))).isNull();
	}

	private long createEvent(String body) throws Exception {
		String location = mockMvc.perform(post("/events")
				.header("Authorization", "Bearer admin")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");
		return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
	}

	private JsonNode receiveMessage() throws Exception {
		org.springframework.amqp.core.Message message = rabbitTemplate.receive(testQueue.getName(),
			TimeUnit.SECONDS.toMillis(5));
		assertThat(message).isNotNull();
		return objectMapper.readTree(message.getBody());
	}

	private void assertOutboxRowMarkedPublished(long eventId) {
		assertThat(outboxRepository.findAll().stream().filter(row -> row.getEventId().equals(eventId)))
			.singleElement()
			.satisfies(row -> assertThat(row.getPublishedAt()).isNotNull());
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class JwtDecoderStubConfiguration {

		@Bean
		@Primary
		JwtDecoder stubJwtDecoder() {
			Jwt admin = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("subject")
				.claim("preferred_username", "subject")
				.claim("realm_access", Map.of("roles", List.of("ADMIN", "USER")))
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(300))
				.build();
			return token -> token.equals("admin") ? admin : null;
		}
	}
}
