package com.passly.booking.adapter.out.messaging;

import java.time.LocalDateTime;

import com.passly.booking.adapter.out.persistence.ReservationOutboxJpaEntity;
import com.passly.booking.adapter.out.persistence.ReservationOutboxJpaRepository;
import com.passly.booking.application.port.TicketReservationPublisher;
import com.passly.booking.domain.Reservation;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Adaptador de salida que implementa {@link TicketReservationPublisher} con el
 * patrón outbox transaccional (ADR-0004): persiste el mensaje en la tabla
 * {@code reservation_outbox} dentro de la misma transacción del caso de uso
 * (ReservationService.bookTransactional), de modo que ninguna Reserva se queda
 * sin notificar. El envío a RabbitMQ lo hace {@link ReservationOutboxPoller}.
 */
@Component
public class ReservationOutboxWriter implements TicketReservationPublisher {

	private final ReservationOutboxJpaRepository outboxRepository;
	private final ObjectMapper objectMapper;

	public ReservationOutboxWriter(ReservationOutboxJpaRepository outboxRepository, ObjectMapper objectMapper) {
		this.outboxRepository = outboxRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public void publish(Reservation reservation) {
		ReservationOutboxJpaEntity entity = new ReservationOutboxJpaEntity();
		entity.setReservationId(reservation.id());
		entity.setReservationType("ReservationCreated");
		entity.setPayload(objectMapper.writeValueAsString(TicketReservedMessage.from(reservation)));
		entity.setCreatedAt(LocalDateTime.now());
		outboxRepository.save(entity);
	}
}
