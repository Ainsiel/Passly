package com.passly.booking.config;

import java.time.Clock;

import com.passly.booking.application.BookingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del contexto Reservas: habilita {@link BookingProperties}
 * (config tipada) y expone un {@link Clock} inyectable para hacer determinista
 * el tiempo en los tests.
 */
@Configuration
@EnableConfigurationProperties(BookingProperties.class)
public class BookingConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}
}
