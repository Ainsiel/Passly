package com.passly.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class EventProjectionTest {

	@Test
	void availabilityIsCapacityMinusReservedTickets() {
		EventProjection projection = projection(500, 120);

		assertThat(projection.available()).isEqualTo(380);
	}

	@Test
	void aSoldOutEventHasZeroAvailability() {
		EventProjection projection = projection(15400, 15400);

		assertThat(projection.available()).isZero();
	}

	@Test
	void rejectsNegativeCapacity() {
		assertThatThrownBy(() -> projection(-1, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("capacity");
	}

	@Test
	void rejectsReservedTicketsOutsideRange() {
		assertThatThrownBy(() -> projection(500, -1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("reservedTickets");

		assertThatThrownBy(() -> projection(500, 501))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("reservedTickets");
	}

	@Test
	void requiresAnId() {
		assertThatThrownBy(() -> new EventProjection(null, "Sin id", LocalDateTime.of(2026, 12, 31, 21, 0),
			new BigDecimal("30.00"), 500, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("id");
	}

	private static EventProjection projection(int capacity, int reservedTickets) {
		return new EventProjection(7L, "Noche de Jazz a la Fresca", LocalDateTime.of(2026, 12, 31, 21, 0),
			new BigDecimal("30.00"), capacity, reservedTickets);
	}
}
