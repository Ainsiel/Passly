package com.passly.notification.application;

import com.passly.notification.adapter.out.email.TicketEmailRenderer;
import org.springframework.stereotype.Service;

/**
 * Caso de uso: entrega el Ticket de una Reserva por email. El mensaje ya valida
 * su estructura en la deserialización; aquí se compone el contenido (asunto y
 * cuerpo HTML) y se delega en el puerto {@link EmailSender}.
 */
@Service
public class EmailDeliveryService {

	private final TicketEmailRenderer renderer;
	private final EmailSender emailSender;

	public EmailDeliveryService(TicketEmailRenderer renderer, EmailSender emailSender) {
		this.renderer = renderer;
		this.emailSender = emailSender;
	}

	public void deliver(TicketReservedMessage message) {
		if (message == null) {
			throw new IllegalArgumentException("Mensaje nulo");
		}
		emailSender.send(message.email(), renderer.subject(message), renderer.render(message));
	}
}
