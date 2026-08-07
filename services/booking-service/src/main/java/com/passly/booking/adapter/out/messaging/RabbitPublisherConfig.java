package com.passly.booking.adapter.out.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ del lado publicador del outbox (ticket #8):
 * declara el exchange propio de Reservas hacia notifications. El conversor
 * JSON (incluido el type mapping de {@code passly:booking:ticket-reserved}) es
 * el bean de {@code RabbitConsumerConfig}; Spring Boot lo aplica al
 * {@code RabbitTemplate} automáticamente.
 */
@Configuration
public class RabbitPublisherConfig {

	@Bean
	TopicExchange bookingsExchange() {
		return new TopicExchange(ReservationTopology.BOOKINGS_EXCHANGE, true, false);
	}
}
