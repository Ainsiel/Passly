package com.passly.booking.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.passly.booking.application.port.EventProjectionRepository;
import com.passly.booking.application.port.ReservationRepository;
import com.passly.booking.application.port.TicketCodeGenerator;
import com.passly.booking.application.port.TicketReservationPublisher;
import com.passly.booking.domain.EventProjection;
import com.passly.booking.domain.Reservation;
import com.passly.booking.domain.Ticket;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for the Reservations context. {@code book} reserves Tickets with
 * optimistic locking (ADR-0003): within a REQUIRES_NEW transaction (invoked
 * through the self-injected proxy) it reads the projection, validates
 * Availability, and atomically decrements; two concurrent Reservations for the
 * last Tickets: one wins and the other gets a conflict. The idempotency key
 * ensures a replay (double-click, retry) returns the same Reservation.
 */
@Service
public class ReservationService {

	private static final int MAX_CONFLICT_ATTEMPTS = 3;

	private final ReservationService self;
	private final ReservationRepository reservationRepository;
	private final EventProjectionRepository eventProjectionRepository;
	private final TicketCodeGenerator codeGenerator;
	private final TicketReservationPublisher reservationPublisher;
	private final BookingProperties properties;
	private final Clock clock;

	public ReservationService(@Lazy ReservationService self, ReservationRepository reservationRepository,
			EventProjectionRepository eventProjectionRepository, TicketCodeGenerator codeGenerator,
			TicketReservationPublisher reservationPublisher, BookingProperties properties, Clock clock) {
		this.self = self;
		this.reservationRepository = reservationRepository;
		this.eventProjectionRepository = eventProjectionRepository;
		this.codeGenerator = codeGenerator;
		this.reservationPublisher = reservationPublisher;
		this.properties = properties;
		this.clock = clock;
	}

	public BookingResult book(String userId, String idempotencyKey, BookReservationCommand command) {
		int attempts = 0;
		while (true) {
			try {
				return self.bookTransactional(userId, idempotencyKey, command);
			}
			catch (ObjectOptimisticLockingFailureException e) {
				// Dos Reservas concurrentes leen la misma versión de la proyección y
				// compiten por los últimos Tickets (ADR-0003): la perdedora reintenta
				// sobre la versión nueva; si ya no queda Disponibilidad, el dominio
				// lanza SoldOutException (409) en el reintento.
				if (++attempts >= MAX_CONFLICT_ATTEMPTS) {
					throw e;
				}
			}
			catch (DataIntegrityViolationException e) {
				// Violación de una restricción única por concurrencia: o bien la
				// idempotency key ya se ha guardado (replay del cliente) o bien el
				// Usuario/Evento ya tiene una Reserva activa (conflicto). Se distingue
				// consultando por la key: si existe, es un reenvío.
				Reservation existing = reservationRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
					.orElseThrow(() -> new DuplicateReservationException(userId, command.eventId()));
				return BookingResult.replayed(existing);
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	BookingResult bookTransactional(String userId, String idempotencyKey, BookReservationCommand command) {
		Optional<Reservation> existing = reservationRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
		if (existing.isPresent()) {
			return BookingResult.replayed(existing.get());
		}
		if (reservationRepository.hasActiveReservation(userId, command.eventId())) {
			throw new DuplicateReservationException(userId, command.eventId());
		}
		EventProjection reserved = eventProjectionRepository.reserve(command.eventId(), command.quantity())
			.orElseThrow(() -> new EventNotFoundException(command.eventId()));
		List<Ticket> tickets = java.util.stream.IntStream.range(0, command.quantity())
			.mapToObj(i -> {
				String code = codeGenerator.next();
				return new Ticket(code, qrPayload(code));
			})
			.toList();
		LocalDateTime now = LocalDateTime.now(clock);
		Reservation reservation = Reservation.book(UUID.randomUUID(), userId, reserved, tickets, command.email(), now,
			properties.getMaxTicketsPerReservation());
		reservationRepository.save(reservation, idempotencyKey);
		reservationPublisher.publish(reservation, reserved);
		return BookingResult.created(reservation);
	}

	@Transactional(readOnly = true)
	public List<Reservation> myReservations(String userId) {
		return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	private String qrPayload(String code) {
		return properties.getTicketQrUrlTemplate().replace("{code}", code);
	}
}
