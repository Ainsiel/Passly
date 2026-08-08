package com.passly.catalog.adapter.out.persistence;

import java.util.Optional;

import com.passly.catalog.application.port.EventRepository;
import com.passly.catalog.domain.Event;
import com.passly.catalog.domain.EventFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador de salida que implementa el puerto {@link EventRepository} sobre
 * Spring Data JPA. Mapea entre la entidad de infraestructura y el dominio.
 */
@Component
public class EventPersistenceAdapter implements EventRepository {

	private final EventJpaRepository eventJpaRepository;

	public EventPersistenceAdapter(EventJpaRepository eventJpaRepository) {
		this.eventJpaRepository = eventJpaRepository;
	}

	@Override
	public Page<Event> search(EventFilter filter, Pageable pageable) {
		return eventJpaRepository.findAll(EventSpecifications.from(filter), pageable)
			.map(EventMapper::toDomain);
	}

	@Override
	public Optional<Event> findById(Long id) {
		return eventJpaRepository.findById(id).map(EventMapper::toDomain);
	}

	@Override
	public Event save(Event event) {
		return EventMapper.toDomain(eventJpaRepository.save(EventMapper.toEntity(event)));
	}

	@Override
	public void deleteById(Long id) {
		eventJpaRepository.deleteById(id);
	}

	@Override
	@Transactional
	public void updateReservedTickets(Long eventId, int reservedTickets) {
		eventJpaRepository.updateReservedTickets(eventId, reservedTickets);
	}
}
