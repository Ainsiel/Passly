package com.passly.booking.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración tipada del contexto Reservas (ROADMAP Fase B: config con
 * {@code @ConfigurationProperties}). {@code maxTicketsPerReservation} es el
 * tope de Tickets por Reserva (default 4); {@code ticketQrUrlTemplate} es la
 * URL canónica que codifica el QR de cada Ticket.
 */
@ConfigurationProperties(prefix = "passly.booking")
public class BookingProperties {

	/** Máximo de Tickets por Reserva. */
	private int maxTicketsPerReservation = 4;

	/** Plantilla de la URL que codifica el QR de un Ticket ({code} se sustituye). */
	private String ticketQrUrlTemplate = "https://passly.local/tickets/{code}";

	public int getMaxTicketsPerReservation() {
		return maxTicketsPerReservation;
	}

	public void setMaxTicketsPerReservation(int maxTicketsPerReservation) {
		this.maxTicketsPerReservation = maxTicketsPerReservation;
	}

	public String getTicketQrUrlTemplate() {
		return ticketQrUrlTemplate;
	}

	public void setTicketQrUrlTemplate(String ticketQrUrlTemplate) {
		this.ticketQrUrlTemplate = ticketQrUrlTemplate;
	}
}
