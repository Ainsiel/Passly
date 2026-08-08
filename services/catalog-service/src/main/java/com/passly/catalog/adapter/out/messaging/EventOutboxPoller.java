package com.passly.catalog.adapter.out.messaging;

import java.time.LocalDateTime;
import java.util.List;

import com.passly.catalog.adapter.out.persistence.EventOutboxJpaEntity;
import com.passly.catalog.adapter.out.persistence.EventOutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Publica a RabbitMQ las filas pendientes del outbox (ADR-0011). Corre cada
 * ~2 segundos (intervalo tipado en {@code OutboxProperties}); si una fila
 * falla se deja pendiente y se reintenta en el siguiente ciclo (entrega
 * at-least-once, que el consumidor de Reservas tolera siendo idempotente).
 * {@code publishPending()} es invocable desde los tests para hacer el ciclo
 * determinista.
 */
@Component
public class EventOutboxPoller {

	private static final Logger log = LoggerFactory.getLogger(EventOutboxPoller.class);

	private final EventOutboxJpaRepository outboxRepository;
	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;

	public EventOutboxPoller(EventOutboxJpaRepository outboxRepository, RabbitTemplate rabbitTemplate,
			ObjectMapper objectMapper) {
		this.outboxRepository = outboxRepository;
		this.rabbitTemplate = rabbitTemplate;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public void publishPending() {
		List<EventOutboxJpaEntity> pending = outboxRepository.findTop100ByPublishedAtIsNullOrderByIdAsc();
		for (EventOutboxJpaEntity row : pending) {
			try {
				EventChangedMessage message = objectMapper.readValue(row.getPayload(), EventChangedMessage.class);
				rabbitTemplate.convertAndSend(RabbitTopology.EVENTS_EXCHANGE, message.routingKey(), message);
				row.setPublishedAt(LocalDateTime.now());
				outboxRepository.save(row);
				log.info("Evento de catálogo publicado: {} (fila outbox {})", message.type(), row.getId());
			}
			catch (Exception e) {
				log.error("No se pudo publicar la fila de outbox {} (evento {}); se reintentará en el próximo ciclo",
					row.getId(), row.getEventId(), e);
			}
		}
	}
}
