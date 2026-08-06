package com.passly.catalog.application;

import com.passly.catalog.application.port.EventRepository;
import com.passly.catalog.domain.Event;
import com.passly.catalog.domain.EventFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de lectura del catálogo: listado paginado/filtrable y detalle.
 * No conoce nada del mundo exterior (HTTP, JPA): solo habla con su puerto.
 */
@Service
@Transactional(readOnly = true)
public class EventQueryService {

	private final EventRepository eventRepository;

	public EventQueryService(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	public Page<Event> search(EventFilter filter, Pageable pageable) {
		return eventRepository.search(filter, pageable);
	}

	public Event getById(Long id) {
		return eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
	}
}
