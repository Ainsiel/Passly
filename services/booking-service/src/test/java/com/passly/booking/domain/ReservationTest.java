package com.passly.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ReservationTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 12, 0);

	private static final String EMAIL = "usuario@passly.local";

	@Test
	void bookBuildsAnActiveReservationWithItsTicketsAndEventSnapshot() {
		EventProjection event = event(500, 120);
		List<Ticket> tickets = tickets(2);

		Reservation reservation = Reservation.book(UUID.randomUUID(), "user-1", event, tickets, EMAIL, NOW, 4);

		assertThat(reservation.userId()).isEqualTo("user-1");
		assertThat(reservation.eventId()).isEqualTo(7L);
		assertThat(reservation.eventName()).isEqualTo("Noche de Jazz a la Fresca");
		assertThat(reservation.startsAt()).isEqualTo(LocalDateTime.of(2026, 12, 31, 21, 0));
		assertThat(reservation.price()).isEqualByComparingTo("30.00");
		assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
		assertThat(reservation.tickets()).hasSize(2);
		assertThat(reservation.createdAt()).isEqualTo(NOW);
		assertThat(reservation.email()).isEqualTo(EMAIL);
	}

	@Test
	void bookRejectsAnEmptyTicketList() {
		assertThatThrownBy(
			() -> Reservation.book(UUID.randomUUID(), "user-1", event(500, 120), List.of(), EMAIL, NOW, 4))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("ticket");
	}

	@Test
	void bookRejectsMoreThanMaxTicketsPerReservation() {
		List<Ticket> fiveTickets = tickets(5);

		assertThatThrownBy(
			() -> Reservation.book(UUID.randomUUID(), "user-1", event(500, 120), fiveTickets, EMAIL, NOW, 4))
			.isInstanceOf(TooManyTicketsException.class)
			.hasMessageContaining("4");
	}

	@Test
	void bookRejectsDuplicateTicketCodesWithinTheReservation() {
		List<Ticket> duplicated = List.of(ticket("MISMO-CODIGO"), ticket("MISMO-CODIGO"));

		assertThatThrownBy(
			() -> Reservation.book(UUID.randomUUID(), "user-1", event(500, 120), duplicated, EMAIL, NOW, 4))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("únicos");
	}

	@Test
	void bookRejectsAnEventThatHasAlreadyStarted() {
		EventProjection pastEvent = new EventProjection(7L, "Noche de Jazz a la Fresca",
			LocalDateTime.of(2026, 8, 1, 21, 0), new BigDecimal("30.00"), 500, 0);

		assertThatThrownBy(() -> Reservation.book(UUID.randomUUID(), "user-1", pastEvent, tickets(1), EMAIL, NOW, 4))
			.isInstanceOf(EventNotBookableException.class)
			.hasMessageContaining("already started");
	}

	@Test
	void bookRejectsABlankUserId() {
		assertThatThrownBy(() -> Reservation.book(UUID.randomUUID(), "   ", event(500, 120), tickets(1), EMAIL, NOW, 4))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("userId");
	}

	@Test
	void bookRequiresAnId() {
		assertThatThrownBy(() -> Reservation.book(null, "user-1", event(500, 120), tickets(1), EMAIL, NOW, 4))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("id");
	}

	@Test
	void bookRejectsABlankEmail() {
		assertThatThrownBy(() -> Reservation.book(UUID.randomUUID(), "user-1", event(500, 120), tickets(1), "   ", NOW, 4))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("email");
	}

	@Test
	void rehydrateCarriesTheRecipientEmail() {
		Reservation reservation = Reservation.rehydrate(UUID.randomUUID(), "user-1", 7L, "Noche de Jazz",
			LocalDateTime.of(2026, 12, 31, 21, 0), new BigDecimal("30.00"), ReservationStatus.ACTIVE, tickets(2),
			EMAIL, NOW);

		assertThat(reservation.email()).isEqualTo(EMAIL);
	}

	private static EventProjection event(int capacity, int reservedTickets) {
		return new EventProjection(7L, "Noche de Jazz a la Fresca", LocalDateTime.of(2026, 12, 31, 21, 0),
			new BigDecimal("30.00"), capacity, reservedTickets);
	}

	private static List<Ticket> tickets(int quantity) {
		return java.util.stream.IntStream.range(0, quantity)
			.mapToObj(i -> ticket("TICKET-" + i))
			.toList();
	}

	private static Ticket ticket(String code) {
		return new Ticket(code, "https://passly.local/tickets/" + code);
	}
}
