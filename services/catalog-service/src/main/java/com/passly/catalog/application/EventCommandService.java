package com.passly.catalog.application;

import com.passly.catalog.application.port.EventPublisher;
import com.passly.catalog.application.port.EventRepository;
import com.passly.catalog.domain.Event;
import com.passly.catalog.domain.EventCreated;
import com.passly.catalog.domain.EventSnapshot;
import com.passly.catalog.domain.EventUpdated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de escritura del catálogo (CRUD admin): crear, sustituir y
 * eliminar Eventos. No conoce nada del mundo exterior (HTTP, JPA): solo habla
 * con sus puertos. El rol ADMIN se aplica en el borde de seguridad.
 * Al crear o editar publica el evento de dominio correspondiente a través de
 * {@link EventPublisher}; eliminar no notifica (la proyección de Reservas no
 * recibe tombstones, ver ADR-0011).
 */
@Service
@Transactional
public class EventCommandService {

	private final EventRepository eventRepository;
	private final EventPublisher eventPublisher;

	public EventCommandService(EventRepository eventRepository, EventPublisher eventPublisher) {
		this.eventRepository = eventRepository;
		this.eventPublisher = eventPublisher;
	}

	public Event create(EventCommand command) {
		Event saved = eventRepository.save(toEvent(null, command, 0));
		eventPublisher.publish(new EventCreated(EventSnapshot.from(saved)));
		return saved;
	}

	public Event update(Long id, EventCommand command) {
		Event existing = eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
		if (command.capacity() < existing.reservedTickets()) {
			throw new EventConflictException("La capacidad " + command.capacity()
				+ " es menor que las " + existing.reservedTickets() + " reservas actuales del evento " + id);
		}
		Event updated = eventRepository.save(toEvent(id, command, existing.reservedTickets()));
		eventPublisher.publish(new EventUpdated(EventSnapshot.from(updated)));
		return updated;
	}

	public void delete(Long id) {
		Event existing = eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
		if (existing.reservedTickets() > 0) {
			throw new EventConflictException("No se puede eliminar el evento " + id
				+ " porque tiene " + existing.reservedTickets() + " tickets reservados");
		}
		eventRepository.deleteById(id);
	}

	private static Event toEvent(Long id, EventCommand command, int reservedTickets) {
		return new Event(id, command.name(), command.description(), command.category(), command.venue(),
			command.startsAt(), command.price(), command.capacity(), reservedTickets);
	}
}
