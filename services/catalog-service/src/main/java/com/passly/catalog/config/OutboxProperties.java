package com.passly.catalog.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración tipada del outbox (ROADMAP Fase B: config con
 * {@code @ConfigurationProperties}). {@code fixedDelay} es el intervalo del
 * poller en milisegundos.
 */
@ConfigurationProperties(prefix = "passly.outbox.poller")
public class OutboxProperties {

	/** Intervalo del poller en milisegundos. */
	private Duration fixedDelay = Duration.ofMillis(2000);

	public Duration getFixedDelay() {
		return fixedDelay;
	}

	public void setFixedDelay(Duration fixedDelay) {
		this.fixedDelay = fixedDelay;
	}
}
