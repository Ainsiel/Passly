package com.passly.notification;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base de los tests de integración del contexto Notificaciones: levanta un
 * RabbitMQ y un Mailhog (SMTP 1025 / API 8025) reales vía Testcontainers y
 * cablea la conexión con {@code @DynamicPropertySource}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class AbstractMessagingIntegrationTest {

	@Container
	static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.3.4-alpine"));

	@Container
	static final GenericContainer<?> MAILHOG = new GenericContainer<>(DockerImageName.parse("mailhog/mailhog:v1.0.1"))
			.withExposedPorts(1025, 8025)
			.waitingFor(Wait.forHttp("/api/v2/messages").forPort(8025));

	@DynamicPropertySource
	static void messagingProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
		registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
		registry.add("spring.rabbitmq.username", () -> "guest");
		registry.add("spring.rabbitmq.password", () -> "guest");
		registry.add("spring.mail.host", MAILHOG::getHost);
		registry.add("spring.mail.port", () -> MAILHOG.getMappedPort(1025));
	}

	protected final MailhogClient mailhog() {
		return new MailhogClient("http://" + MAILHOG.getHost() + ":" + MAILHOG.getMappedPort(8025));
	}
}
