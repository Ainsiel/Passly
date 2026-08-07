package com.passly.booking.adapter.in.messaging;

import java.util.HashMap;
import java.util.Map;

import com.passly.booking.adapter.out.messaging.ReservationTopology;
import com.passly.booking.adapter.out.messaging.TicketReservedMessage;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuración del consumidor de eventos del catálogo: declara exchange,
 * cola y binding, y un conversor que resuelve los id lógicos de ambos
 * contratos (catalog->booking y booking->notification) a los DTO locales.
 * Spring Boot aplica el conversor al listener container y al
 * {@code RabbitTemplate} automáticamente, de modo que el outbox publica con el
 * mismo mapeo.
 */
@Configuration
public class RabbitConsumerConfig {

	@Bean
	Declarables catalogEventsDeclarables() {
		TopicExchange exchange = new TopicExchange(RabbitTopology.EVENTS_EXCHANGE, true, false);
		Queue queue = new Queue(RabbitTopology.EVENT_PROJECTIONS_QUEUE, true);
		Binding binding = BindingBuilder.bind(queue).to(exchange).with(RabbitTopology.EVENT_ROUTING_PATTERN);
		return new Declarables(exchange, queue, binding);
	}

	@Bean
	JacksonJsonMessageConverter jacksonJsonMessageConverter(JsonMapper jsonMapper) {
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
		DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
		Map<String, Class<?>> idClassMapping = new HashMap<>();
		idClassMapping.put(RabbitTopology.EVENT_CHANGED_TYPE_ID, EventChangedMessage.class);
		idClassMapping.put(ReservationTopology.TICKET_RESERVED_TYPE_ID, TicketReservedMessage.class);
		typeMapper.setIdClassMapping(idClassMapping);
		converter.setJavaTypeMapper(typeMapper);
		return converter;
	}
}
