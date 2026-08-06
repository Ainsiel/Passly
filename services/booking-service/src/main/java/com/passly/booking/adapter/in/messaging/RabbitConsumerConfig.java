package com.passly.booking.adapter.in.messaging;

import java.util.Map;

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
 * cola y binding, y un conversor que resuelve el id lógico del contrato al
 * DTO local. Spring Boot aplica el conversor al listener container y al
 * {@code RabbitTemplate} automáticamente.
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
		typeMapper.setIdClassMapping(Map.of(RabbitTopology.EVENT_CHANGED_TYPE_ID, EventChangedMessage.class));
		converter.setJavaTypeMapper(typeMapper);
		return converter;
	}
}
