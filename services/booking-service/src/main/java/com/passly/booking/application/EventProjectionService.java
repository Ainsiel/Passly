package com.passly.booking.application;

import com.passly.booking.application.port.EventProjectionRepository;
import com.passly.booking.domain.EventProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso que aplica una instantánea de Evento a la proyección del
 * contexto Reservas (ADR-0011). No conoce el wire de RabbitMQ: recibe el
 * objeto de dominio ya mapeado por el adaptador entrante. Al ejecutarse
 * dentro de una transacción, la lectura del consumidor más el upsert
 * convergen atómicamente.
 */
@Service
@Transactional
public class EventProjectionService {

	private final EventProjectionRepository repository;

	public EventProjectionService(EventProjectionRepository repository) {
		this.repository = repository;
	}

	public void upsert(EventProjection projection) {
		repository.upsert(projection);
	}
}
