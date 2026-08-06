package com.passly.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class EventTest {

	@Test
	void availabilityIsCapacityMinusReservedTickets() {
		assertThat(event(100, 30).available()).isEqualTo(70);
	}

	@Test
	void soldOutEventHasZeroAvailability() {
		assertThat(event(100, 100).available()).isZero();
	}

	@Test
	void eventWithNoReservationsHasFullAvailability() {
		assertThat(event(100, 0).available()).isEqualTo(100);
	}

	@Test
	void rejectsReservedTicketsAboveCapacity() {
		assertThatIllegalArgumentException().isThrownBy(() -> event(100, 101));
	}

	@Test
	void rejectsNegativeReservedTickets() {
		assertThatIllegalArgumentException().isThrownBy(() -> event(100, -1));
	}

	@Test
	void rejectsNegativeCapacity() {
		assertThatIllegalArgumentException().isThrownBy(() -> event(-1, 0));
	}

	private static Event event(int capacity, int reservedTickets) {
		return new Event(1L, "Concierto de prueba", "Una descripción de prueba", EventCategory.CONCIERTO,
			"Sala de pruebas", LocalDateTime.of(2026, 9, 12, 20, 0), new BigDecimal("45.00"), capacity, reservedTickets);
	}
}
