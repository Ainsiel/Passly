package com.passly.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.passly.notification.adapter.in.messaging.RabbitTopology;
import com.passly.notification.adapter.in.messaging.TicketReservedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Criterios de aceptación del ticket #8 para el contexto Notificaciones:
 * <ul>
 * <li>AC2 — un evento {@code ticket-reserved} válido entrega un email con el
 * Ticket (código y QR) en Mailhog.</li>
 * <li>AC3 — un mensaje intratable se reintenta (máx. 3 intentos) y termina en
 * la dead-letter queue sin notificarse.</li>
 * <li>AC4 — el contexto arranca sin base de datos.</li>
 * </ul>
 */
class NotificationIntegrationTest extends AbstractMessagingIntegrationTest {

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	ApplicationContext context;

	private MailhogClient mailhog;

	@BeforeEach
	void setUp() {
		mailhog = mailhog();
		mailhog.purge();
	}

	@Test
	void aValidReservationDeliversAnEmailWithTheTicketCodeAndQr() {
		var message = new TicketReservedMessage(UUID.randomUUID(), "comprador@example.com", "Concierto de Verano",
				LocalDateTime.of(2026, 8, 15, 20, 0), new BigDecimal("25.00"),
				List.of(new TicketReservedMessage.TicketData("T-12345", "https://passly.local/tickets/T-12345")));

		rabbitTemplate.convertAndSend(RabbitTopology.BOOKINGS_EXCHANGE,
				RabbitTopology.ROUTING_KEY_RESERVATION_CREATED, message);

		MailhogClient.Email email = mailhog.awaitEmail("comprador@example.com");
		assertThat(email.subject()).isEqualTo("Tu ticket para Concierto de Verano");
		assertThat(email.htmlBody())
				.contains("T-12345")
				.contains("https://passly.local/tickets/T-12345")
				.contains("Concierto de Verano")
				.contains("25.00");
	}

	@Test
	void anIntractableMessageIsRetriedAndEndsInTheDeadLetterQueue() throws Exception {
		String poison = "{\"reservationId\":\"00000000-0000-0000-0000-000000000000\",\"email\":\"\","
				+ "\"eventName\":\"X\",\"startsAt\":\"2026-08-15T20:00:00\",\"price\":25.00,"
				+ "\"tickets\":[{\"code\":\"T-POISON\",\"qr\":\"https://passly.local/tickets/T-POISON\"}]}";

		rabbitTemplate.send(RabbitTopology.BOOKINGS_EXCHANGE, RabbitTopology.ROUTING_KEY_RESERVATION_CREATED,
				MessageBuilder.withBody(poison.getBytes(StandardCharsets.UTF_8))
						.setContentType(MessageProperties.CONTENT_TYPE_JSON)
						.setHeader("__TypeId__", RabbitTopology.TICKET_RESERVED_TYPE_ID)
						.build());

		Message deadLettered = rabbitTemplate.receive(RabbitTopology.TICKET_RESERVED_DLQ, 30_000);
		assertThat(deadLettered).isNotNull();
		assertThat(rabbitTemplate.receive(RabbitTopology.TICKET_RESERVED_QUEUE, 1_000)).isNull();
		assertThat(mailhog.messages()).isEmpty();
	}

	@Test
	void contextStartsWithoutAnyDataSource() {
		assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
	}
}
