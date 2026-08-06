package com.passly.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CatalogEventTest {

	@Test
	void eventSnapshotCapturesTheFieldsTheProjectionNeeds() {
		Event event = new Event(7L, "Noche de Jazz a la Fresca", "Velada de jazz íntima",
			EventCategory.CONCIERTO, "Auditorio", LocalDateTime.of(2026, 12, 31, 21, 0),
			new BigDecimal("30.00"), 500, 120);

		EventSnapshot snapshot = EventSnapshot.from(event);

		assertThat(snapshot.id()).isEqualTo(7L);
		assertThat(snapshot.name()).isEqualTo("Noche de Jazz a la Fresca");
		assertThat(snapshot.startsAt()).isEqualTo(LocalDateTime.of(2026, 12, 31, 21, 0));
		assertThat(snapshot.price()).isEqualByComparingTo(new BigDecimal("30.00"));
		assertThat(snapshot.capacity()).isEqualTo(500);
		assertThat(snapshot.reservedTickets()).isEqualTo(120);
		assertThat(snapshot.available()).isEqualTo(380);
	}

	@Test
	void eventCreatedCarriesItsTypeAndSnapshot() {
		EventSnapshot snapshot = snapshot(500, 120);

		EventCreated event = new EventCreated(snapshot);

		assertThat(event.type()).isEqualTo("EventCreated");
		assertThat(event.snapshot()).isSameAs(snapshot);
	}

	@Test
	void eventUpdatedCarriesItsTypeAndSnapshot() {
		EventSnapshot snapshot = snapshot(600, 120);

		EventUpdated event = new EventUpdated(snapshot);

		assertThat(event.type()).isEqualTo("EventUpdated");
		assertThat(event.snapshot()).isSameAs(snapshot);
	}

	@Test
	void snapshotRejectsNegativeCapacity() {
		assertThatThrownBy(() -> snapshot(0, -1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("capacity");
	}

	@Test
	void snapshotRejectsReservedTicketsOutsideRange() {
		assertThatThrownBy(() -> snapshot(500, -1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("reservedTickets");

		assertThatThrownBy(() -> snapshot(500, 501))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("reservedTickets");
	}

	@Test
	void snapshotRequiresAnId() {
		assertThatThrownBy(() -> new EventSnapshot(null, "Sin id", LocalDateTime.of(2026, 12, 31, 21, 0),
			new BigDecimal("30.00"), 500, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("id");
	}

	private static EventSnapshot snapshot(int capacity, int reservedTickets) {
		return new EventSnapshot(7L, "Noche de Jazz a la Fresca", LocalDateTime.of(2026, 12, 31, 21, 0),
			new BigDecimal("30.00"), capacity, reservedTickets);
	}
}
