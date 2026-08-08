package com.passly.catalog.adapter.in.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del consumidor de reservas desde booking-service: declara la
 * cola {@code catalog.reservation-created} vinculada al exchange
 * {@code passly.bookings}. El conversor JSON se configura en
 * {@code RabbitMqConfig} (fusionado con el type mapping del publicador).
 */
@Configuration
public class RabbitConsumerConfig {

	@Bean
	Declarables catalogAvailabilityDeclarables() {
		TopicExchange exchange = new TopicExchange(RabbitTopology.BOOKINGS_EXCHANGE, true, false);
		Queue queue = new Queue(RabbitTopology.CATALOG_AVAILABILITY_QUEUE, true, false, false);
		Binding binding = BindingBuilder.bind(queue).to(exchange)
			.with(RabbitTopology.ROUTING_KEY_RESERVATION_CREATED);
		return new Declarables(exchange, queue, binding);
	}
}
