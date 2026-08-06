package com.passly.catalog.application;

import com.passly.catalog.application.port.EventRepository;
import com.passly.catalog.domain.Event;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de escritura del catálogo (CRUD admin): crear, sustituir y
 * eliminar Eventos. No conoce nada del mundo exterior (HTTP, JPA): solo habla
 * con su puerto. El rol ADMIN se aplica en el borde de seguridad.
 */
@Service
@Transactional
public class EventCommandService {

	private final EventRepository eventRepository;

	public EventCommandService(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	public Event create(EventCommand command) {
		return eventRepository.save(toEvent(null, command, 0));
	}

	public Event update(Long id, EventCommand command) {
		Event existing = eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
		if (command.capacity() < existing.reservedTickets()) {
			throw new EventConflictException("La capacidad " + command.capacity()
				+ " es menor que las " + existing.reservedTickets() + " reservas actuales del evento " + id);
		}
		return eventRepository.save(toEvent(id, command, existing.reservedTickets()));
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
