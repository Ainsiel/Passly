package com.passly.booking.adapter.out.messaging;

import java.time.LocalDateTime;
import java.util.List;

import com.passly.booking.adapter.out.persistence.ReservationOutboxJpaEntity;
import com.passly.booking.adapter.out.persistence.ReservationOutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Publica a RabbitMQ las filas pendientes del outbox de Reservas (ADR-0004,
 * ticket #8). Corre cada ~2 segundos (intervalo tipado en
 * {@code OutboxProperties}); si una fila falla se deja pendiente y se
 * reintenta en el siguiente ciclo (entrega at-least-once, que el consumidor de
 * notifications tolera con su cola idempotente/reintento). {@code publishPending()}
 * es invocable desde los tests para hacer el ciclo determinista.
 */
@Component
public class ReservationOutboxPoller {

	private static final Logger log = LoggerFactory.getLogger(ReservationOutboxPoller.class);

	private final ReservationOutboxJpaRepository outboxRepository;
	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;

	public ReservationOutboxPoller(ReservationOutboxJpaRepository outboxRepository, RabbitTemplate rabbitTemplate,
			ObjectMapper objectMapper) {
		this.outboxRepository = outboxRepository;
		this.rabbitTemplate = rabbitTemplate;
		this.objectMapper = objectMapper;
	}

	public void publishPending() {
		List<ReservationOutboxJpaEntity> pending = outboxRepository.findTop100ByPublishedAtIsNullOrderByIdAsc();
		for (ReservationOutboxJpaEntity row : pending) {
			try {
				TicketReservedMessage message = objectMapper.readValue(row.getPayload(), TicketReservedMessage.class);
				rabbitTemplate.convertAndSend(ReservationTopology.BOOKINGS_EXCHANGE,
					ReservationTopology.ROUTING_KEY_RESERVATION_CREATED, message);
				row.setPublishedAt(LocalDateTime.now());
				outboxRepository.save(row);
				log.info("Reserva publicada para notificación: {} (fila outbox {})", message.reservationId(), row.getId());
			}
			catch (Exception e) {
				log.error("No se pudo publicar la fila de outbox {} (reserva {}); se reintentará en el próximo ciclo",
					row.getId(), row.getReservationId(), e);
			}
		}
	}
}
