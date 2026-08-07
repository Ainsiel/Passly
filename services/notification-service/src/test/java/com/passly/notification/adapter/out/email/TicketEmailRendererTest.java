package com.passly.notification.adapter.out.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.passly.notification.application.TicketReservedMessage;
import com.passly.notification.config.NotificationProperties;
import org.junit.jupiter.api.Test;

class TicketEmailRendererTest {

	private final NotificationProperties properties = new NotificationProperties();
	private final TicketEmailRenderer renderer = new TicketEmailRenderer(properties);

	@Test
	void rendersTheEventAndEachTicketWithItsCodeAndQrLink() {
		var message = new TicketReservedMessage(UUID.randomUUID(), "comprador@example.com", "Concierto de Verano",
				LocalDateTime.of(2026, 8, 15, 20, 0), new BigDecimal("25.00"),
				List.of(new TicketReservedMessage.TicketData("T-12345", "https://passly.local/tickets/T-12345"),
						new TicketReservedMessage.TicketData("T-12346", "https://passly.local/tickets/T-12346")));

		String html = renderer.render(message);

		assertThat(html)
				.contains("Concierto de Verano")
				.contains("T-12345")
				.contains("https://passly.local/tickets/T-12345")
				.contains("T-12346")
				.contains("https://passly.local/tickets/T-12346");
	}

	@Test
	void subjectUsesTheEventName() {
		var message = new TicketReservedMessage(UUID.randomUUID(), "comprador@example.com", "Festival Nocturno",
				LocalDateTime.of(2026, 9, 1, 22, 0), new BigDecimal("35.00"),
				List.of(new TicketReservedMessage.TicketData("T-1", "https://passly.local/tickets/T-1")));

		assertThat(renderer.subject(message)).isEqualTo("Tu ticket para Festival Nocturno");
	}

	@Test
	void escapesHtmlInTheEventName() {
		var message = new TicketReservedMessage(UUID.randomUUID(), "comprador@example.com", "<script>",
				LocalDateTime.of(2026, 9, 1, 22, 0), new BigDecimal("35.00"),
				List.of(new TicketReservedMessage.TicketData("T-1", "https://passly.local/tickets/T-1")));

		assertThat(renderer.render(message)).contains("&lt;script&gt;").doesNotContain("<script>");
	}
}
