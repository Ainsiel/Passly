package com.passly.booking.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import com.passly.booking.application.port.EventProjectionRepository;
import com.passly.booking.domain.EventProjection;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que implementa {@link EventProjectionRepository} sobre
 * Spring Data JPA. El upsert atómico ({@code ON CONFLICT DO UPDATE}) hace el
 * consumidor idempotente: duplicados y reentregas convergen al mismo estado.
 */
@Component
public class EventProjectionPersistenceAdapter implements EventProjectionRepository {

	private final EventProjectionJpaRepository jpaRepository;

	public EventProjectionPersistenceAdapter(EventProjectionJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public void upsert(EventProjection projection) {
		jpaRepository.upsert(projection.id(), projection.name(), projection.startsAt(), projection.price(),
			projection.capacity(), projection.reservedTickets());
	}

	@Override
	public Optional<EventProjection> reserve(Long eventId, int quantity) {
		Optional<EventProjectionJpaEntity> found = jpaRepository.findById(eventId);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		EventProjectionJpaEntity entity = found.get();
		EventProjection reserved = toDomain(entity).reserve(quantity);
		entity.setReservedTickets(reserved.reservedTickets());
		entity.setUpdatedAt(LocalDateTime.now());
		return Optional.of(reserved);
	}

	private EventProjection toDomain(EventProjectionJpaEntity entity) {
		return new EventProjection(entity.getEventId(), entity.getName(), entity.getStartsAt(), entity.getPrice(),
			entity.getCapacity(), entity.getReservedTickets());
	}
}
