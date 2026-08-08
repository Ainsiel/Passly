package com.passly.catalog.adapter.out.messaging;

import java.util.Map;

import com.passly.catalog.adapter.in.messaging.RabbitTopology;
import com.passly.catalog.adapter.in.messaging.TicketReservedMessage;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuración de RabbitMQ del lado publicador: declara el exchange de
 * eventos y el conversor JSON. El type mapping usa un id lógico
 * (ADR-0011) para que el FQCN del DTO del catálogo no viaje en el header.
 * Incluye también el mapping para mensajes entrantes de booking-service.
 */
@Configuration
public class RabbitMqConfig {

	@Bean
	TopicExchange eventsExchange() {
		return new TopicExchange(com.passly.catalog.adapter.out.messaging.RabbitTopology.EVENTS_EXCHANGE, true, false);
	}

	@Bean
	JacksonJsonMessageConverter jacksonJsonMessageConverter(JsonMapper jsonMapper) {
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(jsonMapper);
		DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
		typeMapper.setIdClassMapping(Map.of(
			com.passly.catalog.adapter.out.messaging.RabbitTopology.EVENT_CHANGED_TYPE_ID, EventChangedMessage.class,
			RabbitTopology.TICKET_RESERVED_TYPE_ID, TicketReservedMessage.class));
		converter.setJavaTypeMapper(typeMapper);
		return converter;
	}
}
