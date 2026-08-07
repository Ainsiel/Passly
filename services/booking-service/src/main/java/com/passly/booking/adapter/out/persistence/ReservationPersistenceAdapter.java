package com.passly.booking.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.passly.booking.application.port.ReservationRepository;
import com.passly.booking.domain.Reservation;
import com.passly.booking.domain.ReservationStatus;
import com.passly.booking.domain.Ticket;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador de salida que implementa {@link ReservationRepository} sobre Spring
 * Data JPA. La idempotency key y la unicidad de Reserva activa las refuerzan
 * las restricciones de la BD: una segunda escritura concurrente con la misma
 * key (o el mismo Usuario/Evento activo) lanza {@code DuplicateKeyException},
 * que {@code ReservationService} traduce en replay o conflicto.
 */
@Component
public class ReservationPersistenceAdapter implements ReservationRepository {

	private final ReservationJpaRepository jpaRepository;

	public ReservationPersistenceAdapter(ReservationJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Reservation> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey) {
		return jpaRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey).map(this::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasActiveReservation(String userId, Long eventId) {
		return jpaRepository.existsByUserIdAndEventIdAndStatus(userId, eventId, ReservationStatus.ACTIVE);
	}

	@Override
	@Transactional
	public Reservation save(Reservation reservation, String idempotencyKey) {
		ReservationJpaEntity entity = toEntity(reservation, idempotencyKey);
		return toDomain(jpaRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Reservation> findByUserIdOrderByCreatedAtDesc(String userId) {
		return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDomain).toList();
	}

	private ReservationJpaEntity toEntity(Reservation reservation, String idempotencyKey) {
		ReservationJpaEntity entity = new ReservationJpaEntity();
		entity.setId(reservation.id());
		entity.setUserId(reservation.userId());
		entity.setEmail(reservation.email());
		entity.setEventId(reservation.eventId());
		entity.setEventName(reservation.eventName());
		entity.setStartsAt(reservation.startsAt());
		entity.setPrice(reservation.price());
		entity.setStatus(reservation.status());
		entity.setIdempotencyKey(idempotencyKey);
		entity.setCreatedAt(reservation.createdAt());
		reservation.tickets().forEach(ticket -> {
			TicketJpaEntity ticketEntity = new TicketJpaEntity();
			ticketEntity.setId(UUID.randomUUID());
			ticketEntity.setReservation(entity);
			ticketEntity.setCode(ticket.code());
			ticketEntity.setQr(ticket.qr());
			ticketEntity.setCreatedAt(reservation.createdAt());
			entity.addTicket(ticketEntity);
		});
		return entity;
	}

	private Reservation toDomain(ReservationJpaEntity entity) {
		List<Ticket> tickets = entity.getTickets().stream()
			.map(ticket -> new Ticket(ticket.getCode(), ticket.getQr()))
			.toList();
		return Reservation.rehydrate(entity.getId(), entity.getUserId(), entity.getEventId(), entity.getEventName(),
			entity.getStartsAt(), entity.getPrice(), entity.getStatus(), tickets, entity.getEmail(),
			entity.getCreatedAt());
	}
}
