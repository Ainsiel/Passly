package com.passly.notification.adapter.out.email;

import com.passly.notification.application.TicketReservedMessage;
import com.passly.notification.config.NotificationProperties;
import org.springframework.stereotype.Component;

/**
 * Renderiza el email HTML del Ticket (ADR-0010): evento, fecha, precio y un
 * Ticket por entrada con su código y el payload QR (una URL canónica) enlazado.
 * Es una función pura sobre el mensaje, testeable sin infraestructura.
 */
@Component
public class TicketEmailRenderer {

	private final NotificationProperties properties;

	public TicketEmailRenderer(NotificationProperties properties) {
		this.properties = properties;
	}

	public String subject(TicketReservedMessage message) {
		return properties.getEmail().getSubjectTemplate().replace("{eventName}", message.eventName());
	}

	public String render(TicketReservedMessage message) {
		StringBuilder ticketsHtml = new StringBuilder();
		for (TicketReservedMessage.TicketData ticket : message.tickets()) {
			ticketsHtml.append("""
					<li>
						<code>%s</code>
						<a href="%s" title="Código QR del ticket">QR</a>
					</li>
					""".formatted(escape(ticket.code()), escape(ticket.qr())));
		}
		return """
				<!DOCTYPE html>
				<html lang="es">
				<head>
					<meta charset="utf-8">
					<title>Tu ticket para %s</title>
				</head>
				<body>
					<h1>Tu ticket para %s</h1>
					<p><strong>%s</strong></p>
					<p>%s</p>
					<p>Pagaste %s EUR</p>
					<ul>
				%s
					</ul>
					<p><a href="%s">Ver tus reservas</a></p>
				</body>
				</html>
				""".formatted(escape(message.eventName()), escape(message.eventName()), escape(message.eventName()),
				message.startsAt(), message.price(), ticketsHtml, properties.getEmail().getMyReservationsUrl());
	}

	private String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}
