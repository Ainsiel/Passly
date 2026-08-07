package com.passly.notification.adapter.in.messaging;

import com.passly.notification.application.EmailDeliveryService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Entrada hexagonal (AC3): recibe el evento {@code ticket-reserved} y delega en
 * el caso de uso. Si el mensaje es inválido la excepción se propaga y el
 * listener container reintenta según {@code spring.rabbitmq.listener.simple.retry}
 * (3 intentos) y, al agotarse, descarta el mensaje en la DLQ.
 */
@Component
public class TicketReservedListener {

	private final EmailDeliveryService emailDeliveryService;

	public TicketReservedListener(EmailDeliveryService emailDeliveryService) {
		this.emailDeliveryService = emailDeliveryService;
	}

	@RabbitListener(queues = RabbitTopology.TICKET_RESERVED_QUEUE)
	public void onTicketReserved(TicketReservedMessage message) {
		emailDeliveryService.deliver(message);
	}
}
