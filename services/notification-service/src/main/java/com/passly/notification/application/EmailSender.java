package com.passly.notification.application;

/**
 * Puerto de salida del contexto Notificaciones: entrega un email con el Ticket.
 * La implementación concreta envía por SMTP a Mailhog en desarrollo (ADR-0004).
 */
public interface EmailSender {

	void send(String to, String subject, String htmlBody);
}
