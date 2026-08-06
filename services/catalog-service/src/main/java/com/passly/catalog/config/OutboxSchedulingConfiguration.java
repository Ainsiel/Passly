package com.passly.catalog.config;

import com.passly.catalog.adapter.out.messaging.EventOutboxPoller;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Agenda el poller del outbox con el intervalo tipado en
 * {@link OutboxProperties} (ROADMAP Fase B: config con
 * {@code @ConfigurationProperties}). Requiere {@code @EnableScheduling}.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxSchedulingConfiguration implements SchedulingConfigurer {

	private final OutboxProperties properties;
	private final EventOutboxPoller poller;

	public OutboxSchedulingConfiguration(OutboxProperties properties, EventOutboxPoller poller) {
		this.properties = properties;
		this.poller = poller;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addFixedDelayTask(poller::publishPending, properties.getFixedDelay().toMillis());
	}
}
