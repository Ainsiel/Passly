package com.passly.booking.config;

import com.passly.booking.adapter.out.messaging.ReservationOutboxPoller;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Agenda el poller del outbox de Reservas con el intervalo tipado en
 * {@link OutboxProperties} (ticket #8). Requiere {@code @EnableScheduling}.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class OutboxSchedulingConfiguration implements SchedulingConfigurer {

	private final OutboxProperties properties;
	private final ReservationOutboxPoller poller;

	public OutboxSchedulingConfiguration(OutboxProperties properties, ReservationOutboxPoller poller) {
		this.properties = properties;
		this.poller = poller;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.addFixedDelayTask(poller::publishPending, properties.getFixedDelay().toMillis());
	}
}
