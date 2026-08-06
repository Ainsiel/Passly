package com.passly.booking.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.passly.booking.application.port.EventProjectionRepository;
import com.passly.booking.application.port.ReservationRepository;
import com.passly.booking.application.port.TicketCodeGenerator;
import com.passly.booking.domain.EventProjection;
import com.passly.booking.domain.Reservation;
import com.passly.booking.domain.Ticket;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso del contexto Reservas. {@code book} reserva Tickets con
 * optimistic locking (ADR-0003): dentro de una transacción REQUIRES_NEW
 * (invocada a través del proxy self-inyectado) se lee la proyección, se valida
 * la Disponibilidad y se descuenta atómicamente; dos Reservas concurrentes por
 * los últimos Tickets: una gana y la otra recibe conflicto. La idempotency key
 * hace que un reenvío (doble clic, retry) devuelva la misma Reserva.
 */
@Service
public class ReservationService {

	private final ReservationService self;
	private final ReservationRepository reservationRepository;
	private final EventProjectionRepository eventProjectionRepository;
	private final TicketCodeGenerator codeGenerator;
	private final BookingProperties properties;
	private final Clock clock;

	public ReservationService(@Lazy ReservationService self, ReservationRepository reservationRepository,
			EventProjectionRepository eventProjectionRepository, TicketCodeGenerator codeGenerator,
			BookingProperties properties, Clock clock) {
		this.self = self;
		this.reservationRepository = reservationRepository;
		this.eventProjectionRepository = eventProjectionRepository;
		this.codeGenerator = codeGenerator;
		this.properties = properties;
		this.clock = clock;
	}

	public BookingResult book(String userId, String idempotencyKey, BookReservationCommand command) {
		try {
			return self.bookTransactional(userId, idempotencyKey, command);
		}
		catch (DuplicateKeyException e) {
			// Dos requests concurrentes con la misma key (o el mismo Usuario/Evento):
			// la restricción única ya se ha guardado; se devuelve la Reserva ganadora.
			Reservation existing = reservationRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
				.orElseThrow(() -> new DuplicateReservationException(userId, command.eventId()));
			return BookingResult.replayed(existing);
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
		Reservation reservation = Reservation.book(UUID.randomUUID(), userId, reserved, tickets, now,
			properties.getMaxTicketsPerReservation());
		reservationRepository.save(reservation, idempotencyKey);
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
