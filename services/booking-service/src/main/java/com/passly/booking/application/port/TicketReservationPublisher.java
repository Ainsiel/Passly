package com.passly.booking.application.port;

import com.passly.booking.domain.EventProjection;
import com.passly.booking.domain.Reservation;

/**
 * Puerto de salida para notificar a otros contextos de una Reserva creada
 * (ticket #8). Lo implementa el adaptador de mensajería con el outbox
 * transaccional (ADR-0004): la fila de outbox se escribe en la misma
 * transacción que la Reserva, de modo que ningún Ticket se queda sin
 * notificar. El envío a RabbitMQ lo hace el poller.
 */
public interface TicketReservationPublisher {

	void publish(Reservation reservation, EventProjection event);
}
