package com.passly.catalog.application.port;

import java.util.Optional;

import com.passly.catalog.domain.Event;
import com.passly.catalog.domain.EventFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de salida para la persistencia de Eventos. Lo implementa el adaptador
 * de infraestructura ({@code adapter.out.persistence}).
 */
public interface EventRepository {

	Page<Event> search(EventFilter filter, Pageable pageable);

	Optional<Event> findById(Long id);

	Event save(Event event);

	void deleteById(Long id);
}
