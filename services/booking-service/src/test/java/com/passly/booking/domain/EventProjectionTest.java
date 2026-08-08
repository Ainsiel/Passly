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

	@Test
	void reserveIncrementsReservedTicketsAndKeepsTheSnapshot() {
		EventProjection projection = projection(500, 120);

		EventProjection reserved = projection.reserve(2);

		assertThat(reserved.reservedTickets()).isEqualTo(122);
		assertThat(reserved.available()).isEqualTo(378);
		assertThat(reserved.id()).isEqualTo(projection.id());
		assertThat(reserved.name()).isEqualTo(projection.name());
		assertThat(reserved.price()).isEqualByComparingTo(projection.price());
	}

	@Test
	void reserveRejectsANonPositiveQuantity() {
		EventProjection projection = projection(500, 120);

		assertThatThrownBy(() -> projection.reserve(0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("quantity");

		assertThatThrownBy(() -> projection.reserve(-1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("quantity");
	}

	@Test
	void reserveRejectsMoreThanAvailableTickets() {
		EventProjection projection = projection(500, 498);

		assertThatThrownBy(() -> projection.reserve(3))
			.isInstanceOf(SoldOutException.class)
			.hasMessageContaining("sold out");
	}

	private static EventProjection projection(int capacity, int reservedTickets) {
		return new EventProjection(7L, "Noche de Jazz a la Fresca", LocalDateTime.of(2026, 12, 31, 21, 0),
			new BigDecimal("30.00"), capacity, reservedTickets);
	}
}
