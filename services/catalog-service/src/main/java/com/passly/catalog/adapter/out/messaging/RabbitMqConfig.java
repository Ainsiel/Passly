package com.passly.catalog.adapter.out.messaging;

import java.util.Map;

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
 * Spring Boot aplica el conversor al {@code RabbitTemplate} automáticamente.
 */
@Configuration
public class RabbitMqConfig {

	@Bean
	TopicExchange eventsExchange() {
		return new TopicExchange(RabbitTopology.EVENTS_EXCHANGE, true, false);
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
