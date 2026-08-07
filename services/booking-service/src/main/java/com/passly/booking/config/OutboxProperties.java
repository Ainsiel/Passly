package com.passly.booking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración tipada del outbox de Reservas (ticket #8), espejo de la del
 * catálogo. {@code fixedDelay} es el intervalo del poller en milisegundos.
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
