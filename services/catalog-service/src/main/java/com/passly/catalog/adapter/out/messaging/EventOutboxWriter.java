package com.passly.catalog.adapter.out.messaging;

import java.time.LocalDateTime;

import com.passly.catalog.adapter.out.persistence.EventOutboxJpaEntity;
import com.passly.catalog.adapter.out.persistence.EventOutboxJpaRepository;
import com.passly.catalog.application.port.EventPublisher;
import com.passly.catalog.domain.CatalogEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Adaptador de salida que implementa {@link EventPublisher} con el patrón
 * outbox transaccional (ADR-0011): persiste el mensaje en la tabla
 * {@code event_outbox} dentro de la misma transacción del caso de uso, de modo
 * que ningún cambio del catálogo se queda sin notificar. El envío a RabbitMQ
 * lo hace {@link EventOutboxPoller}.
 */
@Component
public class EventOutboxWriter implements EventPublisher {

	private final EventOutboxJpaRepository outboxRepository;
	private final ObjectMapper objectMapper;

	public EventOutboxWriter(EventOutboxJpaRepository outboxRepository, ObjectMapper objectMapper) {
		this.outboxRepository = outboxRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public void publish(CatalogEvent event) {
		EventOutboxJpaEntity entity = new EventOutboxJpaEntity();
		entity.setEventId(event.snapshot().id());
		entity.setEventType(event.type());
		entity.setPayload(objectMapper.writeValueAsString(EventChangedMessage.from(event)));
		entity.setCreatedAt(LocalDateTime.now());
		outboxRepository.save(entity);
	}
}
