package com.passly.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Contexto Notificaciones: consume el evento {@code booking.reservation.created}
 * desde RabbitMQ y entrega el Ticket por email vía SMTP (ticket #8). No tiene
 * base de datos propia (ADR-0004): toda su lógica es de aplicación.
 */
@SpringBootApplication
public class NotificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationApplication.class, args);
	}
}
