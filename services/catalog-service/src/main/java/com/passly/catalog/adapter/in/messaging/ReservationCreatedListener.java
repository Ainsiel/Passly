package com.passly.catalog.adapter.in.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.passly.catalog.application.port.EventRepository;

/**
 * Consumidor del evento {@code ticket-reserved} que publica booking-service.
 * Actualiza la columna {@code reserved_tickets} en la tabla events para que
 * la disponibilidad mostrada al usuario sea consistente con las reservas
 * reales.
 */
@Component
public class ReservationCreatedListener {

	private static final Logger log = LoggerFactory.getLogger(ReservationCreatedListener.class);

	private final EventRepository eventRepository;

	public ReservationCreatedListener(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	@RabbitListener(queues = RabbitTopology.CATALOG_AVAILABILITY_QUEUE)
	public void onReservationCreated(TicketReservedMessage message) {
		if (message.eventId() == null || message.reservedTickets() == null) {
			log.warn("Mensaje sin eventId o reservedTickets, se descarta: {}", message.reservationId());
			return;
		}
		eventRepository.updateReservedTickets(message.eventId(), message.reservedTickets());
		log.info("Disponibilidad actualizada: evento {} → {} tickets reservados", message.eventId(),
			message.reservedTickets());
	}
}
