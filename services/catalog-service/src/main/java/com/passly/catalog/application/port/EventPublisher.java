package com.passly.catalog.application.port;

import com.passly.catalog.domain.CatalogEvent;

/**
 * Puerto de salida para notificar a otros contextos de los cambios en el
 * catálogo. Lo implementa el adaptador de mensajería (outbox transaccional),
 * de modo que el evento de dominio y el cambio del Evento se persistan en la
 * misma transacción.
 */
public interface EventPublisher {

	void publish(CatalogEvent event);
}
