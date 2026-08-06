package com.passly.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TicketTest {

	@Test
	void aTicketCarriesItsUniqueCodeAndQrPayload() {
		Ticket ticket = new Ticket("A1B2C3D4E5F6", "https://passly.local/tickets/A1B2C3D4E5F6");

		assertThat(ticket.code()).isEqualTo("A1B2C3D4E5F6");
		assertThat(ticket.qr()).isEqualTo("https://passly.local/tickets/A1B2C3D4E5F6");
	}

	@Test
	void rejectsABlankCode() {
		assertThatThrownBy(() -> new Ticket(null, "https://passly.local/tickets/x"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("code");

		assertThatThrownBy(() -> new Ticket("   ", "https://passly.local/tickets/x"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("code");
	}

	@Test
	void rejectsABlankQrPayload() {
		assertThatThrownBy(() -> new Ticket("A1B2C3D4E5F6", null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("qr");

		assertThatThrownBy(() -> new Ticket("A1B2C3D4E5F6", "  "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("qr");
	}
}
