package com.passly.booking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.passly.booking.application.port.EventProjectionRepository;
import com.passly.booking.application.port.ReservationRepository;
import com.passly.booking.application.port.TicketCodeGenerator;
import com.passly.booking.application.port.TicketReservationPublisher;
import com.passly.booking.domain.EventNotBookableException;
import com.passly.booking.domain.EventProjection;
import com.passly.booking.domain.Reservation;
import com.passly.booking.domain.ReservationStatus;
import com.passly.booking.domain.SoldOutException;
import com.passly.booking.domain.TooManyTicketsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 12, 0);

	private final Clock clock = Clock.fixed(Instant.parse("2026-08-06T12:00:00Z"), ZoneOffset.UTC);
	private final FakeEventProjectionRepository eventRepository = new FakeEventProjectionRepository();
	private final FakeReservationRepository reservationRepository = new FakeReservationRepository();
	private final FakeTicketReservationPublisher publisher = new FakeTicketReservationPublisher();
	private final SequentialCodeGenerator codeGenerator = new SequentialCodeGenerator();
	private final BookingProperties properties = new BookingProperties();

	private ReservationService service;

	@BeforeEach
	void setUp() {
		properties.setMaxTicketsPerReservation(4);
		properties.setTicketQrUrlTemplate("https://passly.local/tickets/{code}");
		ReservationService self = new ReservationService(null, reservationRepository, eventRepository, codeGenerator,
			publisher, properties, clock);
		service = new ReservationService(self, reservationRepository, eventRepository, codeGenerator, publisher,
			properties, clock);
	}

	@Test
	void bookCreatesAReservationDecrementsAvailabilityAndReturnsCreated() {
		eventRepository.seed(event(7L, future(2026, 12, 31), 500, 498));

		BookingResult result = service.book("user-1", "key-1", new BookReservationCommand(7L, 2, "usuario@passly.local"));

		assertThat(result.replayed()).isFalse();
		Reservation reservation = result.reservation();
		assertThat(reservation.userId()).isEqualTo("user-1");
		assertThat(reservation.eventId()).isEqualTo(7L);
		assertThat(reservation.eventName()).isEqualTo("Noche de Jazz a la Fresca");
		assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
		assertThat(reservation.tickets()).hasSize(2);
		assertThat(reservation.tickets().get(0).code()).isEqualTo("CODE-1");
		assertThat(reservation.tickets().get(0).qr()).isEqualTo("https://passly.local/tickets/CODE-1");
		assertThat(reservation.tickets().get(1).code()).isEqualTo("CODE-2");
		assertThat(reservation.tickets().get(1).qr()).isEqualTo("https://passly.local/tickets/CODE-2");
		assertThat(eventRepository.projection(7L).orElseThrow().reservedTickets()).isEqualTo(500);
		assertThat(reservationRepository.saved()).hasSize(1);
	}

	@Test
	void replayingWithTheSameIdempotencyKeyReturnsTheExistingReservation() {
		eventRepository.seed(event(7L, future(2026, 12, 31), 500, 498));
		BookingResult first = service.book("user-1", "key-1", new BookReservationCommand(7L, 2, "usuario@passly.local"));

		BookingResult replay = service.book("user-1", "key-1", new BookReservationCommand(7L, 2, "usuario@passly.local"));

		assertThat(replay.replayed()).isTrue();
		assertThat(replay.reservation().id()).isEqualTo(first.reservation().id());
		assertThat(reservationRepository.saved()).hasSize(1);
		assertThat(eventRepository.projection(7L).orElseThrow().reservedTickets()).isEqualTo(500);
	}

	@Test
	void bookThrowsEventNotFoundExceptionForAnUnknownEvent() {
		assertThatThrownBy(() -> service.book("user-1", "key-1", new BookReservationCommand(999L, 1, "usuario@passly.local")))
			.isInstanceOf(EventNotFoundException.class)
			.hasMessageContaining("999");
	}

	@Test
	void bookThrowsSoldOutExceptionWhenAvailabilityIsNotEnough() {
		eventRepository.seed(event(7L, future(2026, 12, 31), 500, 499));

		assertThatThrownBy(() -> service.book("user-1", "key-1", new BookReservationCommand(7L, 2, "usuario@passly.local")))
			.isInstanceOf(SoldOutException.class)
			.hasMessageContaining("sold out");
	}

	@Test
	void bookThrowsDuplicateReservationExceptionWhenTheUserAlreadyHasAnActiveReservation() {
		eventRepository.seed(event(7L, future(2026, 12, 31), 500, 498));
		reservationRepository.seedActiveReservation("user-1", 7L);

		assertThatThrownBy(() -> service.book("user-1", "key-1", new BookReservationCommand(7L, 2, "usuario@passly.local")))
			.isInstanceOf(DuplicateReservationException.class)
			.hasMessageContaining("already has an active reservation");
	}

	@Test
	void bookThrowsEventNotBookableExceptionForAPastEvent() {
		eventRepository.seed(event(7L, LocalDateTime.of(2026, 1, 1, 21, 0), 500, 0));

		assertThatThrownBy(() -> service.book("user-1", "key-1", new BookReservationCommand(7L, 2, "usuario@passly.local")))
			.isInstanceOf(EventNotBookableException.class)
			.hasMessageContaining("already started");
	}

	@Test
	void bookThrowsTooManyTicketsExceptionAboveTheConfiguredMax() {
		eventRepository.seed(event(7L, future(2026, 12, 31), 500, 0));

		assertThatThrownBy(() -> service.book("user-1", "key-1", new BookReservationCommand(7L, 5, "usuario@passly.local")))
			.isInstanceOf(TooManyTicketsException.class)
			.hasMessageContaining("4");
	}

	@Test
	void myReservationsReturnsOnlyTheCallingUsersReservations() {
		eventRepository.seed(event(7L, future(2026, 12, 31), 500, 0));
		service.book("user-1", "key-1", new BookReservationCommand(7L, 1, "usuario@passly.local"));
		service.book("user-2", "key-2", new BookReservationCommand(7L, 2, "usuario@passly.local"));

		List<Reservation> mine = service.myReservations("user-1");

		assertThat(mine).hasSize(1);
		assertThat(mine.get(0).userId()).isEqualTo("user-1");
		assertThat(mine.get(0).tickets()).hasSize(1);
	}

	@Test
	void bookPublishesATicketReservedEventAndAReplayDoesNot() {
		eventRepository.seed(event(7L, future(2026, 12, 31), 500, 498));

		service.book("user-1", "key-1", new BookReservationCommand(7L, 2, "usuario@passly.local"));
		service.book("user-1", "key-1", new BookReservationCommand(7L, 2, "usuario@passly.local"));

		assertThat(publisher.published()).hasSize(1);
		Reservation published = publisher.published().get(0);
		assertThat(published.email()).isEqualTo("usuario@passly.local");
		assertThat(published.tickets()).hasSize(2);
	}

	@Test
	void aConflictedReservationIsNotPublished() {
		eventRepository.seed(event(7L, future(2026, 12, 31), 500, 499));

		assertThatThrownBy(() -> service.book("user-1", "key-1", new BookReservationCommand(7L, 2, "usuario@passly.local")))
			.isInstanceOf(SoldOutException.class);

		assertThat(publisher.published()).isEmpty();
	}

	private static EventProjection event(long id, LocalDateTime startsAt, int capacity, int reservedTickets) {
		return new EventProjection(id, "Noche de Jazz a la Fresca", startsAt, new BigDecimal("30.00"), capacity,
			reservedTickets);
	}

	private static LocalDateTime future(int year, int month, int day) {
		return LocalDateTime.of(year, month, day, 21, 0);
	}

	private static final class FakeEventProjectionRepository implements EventProjectionRepository {

		private final Map<Long, EventProjection> projections = new HashMap<>();

		void seed(EventProjection projection) {
			projections.put(projection.id(), projection);
		}

		Optional<EventProjection> projection(Long eventId) {
			return Optional.ofNullable(projections.get(eventId));
		}

		@Override
		public void upsert(EventProjection projection) {
			projections.put(projection.id(), projection);
		}

		@Override
		public Optional<EventProjection> reserve(Long eventId, int quantity) {
			return Optional.ofNullable(projections.get(eventId)).map(event -> {
				EventProjection reserved = event.reserve(quantity);
				projections.put(eventId, reserved);
				return reserved;
			});
		}
	}

	private static final class FakeReservationRepository implements ReservationRepository {

		private final Map<String, Reservation> byIdempotencyKey = new HashMap<>();
		private final Set<String> activeUserEvent = new HashSet<>();
		private final List<Reservation> saved = new ArrayList<>();

		void seedActiveReservation(String userId, Long eventId) {
			activeUserEvent.add(userId + "|" + eventId);
		}

		List<Reservation> saved() {
			return saved;
		}

		@Override
		public Optional<Reservation> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey) {
			return Optional.ofNullable(byIdempotencyKey.get(userId + "|" + idempotencyKey));
		}

		@Override
		public boolean hasActiveReservation(String userId, Long eventId) {
			return activeUserEvent.contains(userId + "|" + eventId);
		}

		@Override
		public Reservation save(Reservation reservation, String idempotencyKey) {
			saved.add(reservation);
			byIdempotencyKey.put(reservation.userId() + "|" + idempotencyKey, reservation);
			activeUserEvent.add(reservation.userId() + "|" + reservation.eventId());
			return reservation;
		}

		@Override
		public List<Reservation> findByUserIdOrderByCreatedAtDesc(String userId) {
			return saved.stream().filter(reservation -> reservation.userId().equals(userId)).toList();
		}
	}

	private static final class SequentialCodeGenerator implements TicketCodeGenerator {

		private int next = 1;

		@Override
		public String next() {
			return "CODE-" + next++;
		}
	}

	private static final class FakeTicketReservationPublisher implements TicketReservationPublisher {

		private final List<Reservation> published = new ArrayList<>();

		List<Reservation> published() {
			return published;
		}

		@Override
		public void publish(Reservation reservation, EventProjection event) {
			published.add(reservation);
		}
	}
}
