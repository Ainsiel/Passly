package com.passly.notification.adapter.in.messaging;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import com.passly.notification.application.TicketReservedMessage;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuración del consumidor del evento {@code ticket-reserved}: declara el
 * exchange {@code passly.bookings} (idempotente con booking-service), la cola de
 * trabajo con su DLX/DLQ y el binding; y un conversor que resuelve el id lógico
 * del contrato a la copia local del DTO. Spring Boot aplica el conversor al
 * listener container y al {@code RabbitTemplate}, de modo que los tests pueden
 * publicar y consumir con el mismo mapeo.
 */
@Configuration
public class RabbitConsumerConfig {

	@Bean
	Declarables ticketReservedDeclarables() {
		TopicExchange exchange = new TopicExchange(RabbitTopology.BOOKINGS_EXCHANGE, true, false);
		Queue queue = new Queue(RabbitTopology.TICKET_RESERVED_QUEUE, true, false, false,
				Map.of("x-dead-letter-exchange", RabbitTopology.TICKET_RESERVED_DLX,
						"x-dead-letter-routing-key", RabbitTopology.TICKET_RESERVED_DLQ));
		Binding binding = BindingBuilder.bind(queue).to(exchange).with(RabbitTopology.ROUTING_KEY_RESERVATION_CREATED);
		return new Declarables(exchange, queue, binding);
	}

	@Bean
	Declarables ticketReservedDlqDeclarables() {
		TopicExchange dlx = new TopicExchange(RabbitTopology.TICKET_RESERVED_DLX, true, false);
		Queue dlq = new Queue(RabbitTopology.TICKET_RESERVED_DLQ, true);
		Binding binding = BindingBuilder.bind(dlq).to(dlx).with(RabbitTopology.TICKET_RESERVED_DLQ);
		return new Declarables(dlx, dlq, binding);
	}

	@Bean
	JacksonJsonMessageConverter jacksonJsonMessageConverter(JsonMapper jsonMapper) {
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
		DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
		Map<String, Class<?>> idClassMapping = new HashMap<>();
		idClassMapping.put(RabbitTopology.TICKET_RESERVED_TYPE_ID, TicketReservedMessage.class);
		typeMapper.setIdClassMapping(idClassMapping);
		converter.setJavaTypeMapper(typeMapper);
		return converter;
	}
}
