package com.passly.booking.application.port;

import com.passly.booking.domain.EventProjection;

/**
 * Puerto de salida para persistir la proyección de eventos. La operación de
 * upsert debe ser idempotente: aplicar el mismo {@link EventProjection} varias
 * veces deja la proyección con el mismo estado final.
 */
public interface EventProjectionRepository {

	void upsert(EventProjection projection);
}
