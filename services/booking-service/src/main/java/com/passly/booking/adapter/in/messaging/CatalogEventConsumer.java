package com.passly.booking.adapter.in.messaging;

import com.passly.booking.application.EventProjectionService;
import com.passly.booking.domain.EventProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador entrante que consume los mensajes del catálogo y los aplica a la
 * proyección. Es idempotente: {@link EventProjectionService#upsert} converge
 * al mismo estado final aunque el mensaje se entregue duplicado o se vuelva a
 * emitir (ADR-0011). El acuse se produce al retornar; un mensaje malformado se
 * acusa y descarta con un warning para no entrar en bucle de reentrega.
 */
@Component
public class CatalogEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(CatalogEventConsumer.class);

	private final EventProjectionService projectionService;

	public CatalogEventConsumer(EventProjectionService projectionService) {
		this.projectionService = projectionService;
	}

	@RabbitListener(queues = RabbitTopology.EVENT_PROJECTIONS_QUEUE)
	public void onEventChanged(EventChangedMessage message) {
		if (message == null || message.event() == null || message.event().id() == null) {
			log.warn("Mensaje catalog->booking malformado, se descarta: {}", message);
			return;
		}
		if (message.type() == null) {
			log.warn("Mensaje catalog->booking sin tipo, se descarta: {}", message);
			return;
		}
		switch (message.type()) {
			case "EventCreated", "EventUpdated" -> apply(message);
			default -> log.warn("Tipo de mensaje desconocido, se descarta: {}", message.type());
		}
	}

	private void apply(EventChangedMessage message) {
		EventChangedMessage.EventData event = message.event();
		try {
			EventProjection projection = new EventProjection(event.id(), event.name(), event.startsAt(),
				event.price(), event.capacity(), event.reservedTickets());
			projectionService.upsert(projection);
			log.debug("Proyección de evento {} actualizada tras {}", event.id(), message.type());
		}
		catch (IllegalArgumentException e) {
			log.warn("Instantánea inválida en mensaje catalog->booking, se descarta: {}", message, e);
		}
	}
}
