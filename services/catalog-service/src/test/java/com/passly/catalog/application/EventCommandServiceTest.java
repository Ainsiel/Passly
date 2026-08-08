package com.passly.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.passly.catalog.application.port.EventPublisher;
import com.passly.catalog.application.port.EventRepository;
import com.passly.catalog.domain.CatalogEvent;
import com.passly.catalog.domain.Event;
import com.passly.catalog.domain.EventCategory;
import com.passly.catalog.domain.EventCreated;
import com.passly.catalog.domain.EventFilter;
import com.passly.catalog.domain.EventSnapshot;
import com.passly.catalog.domain.EventUpdated;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class EventCommandServiceTest {

	private final FakeRepository repository = new FakeRepository();
	private final FakePublisher publisher = new FakePublisher();
	private final EventCommandService service = new EventCommandService(repository, publisher);

	@Test
	void createPersistsANewEventWithoutReservations() {
		EventCommand command = command("Noche de Jazz a la Fresca", new BigDecimal("30.00"), 500);

		Event created = service.create(command);

		assertThat(created.id()).isNotNull();
		assertThat(created.name()).isEqualTo("Noche de Jazz a la Fresca");
		assertThat(created.category()).isEqualTo(EventCategory.CONCIERTO);
		assertThat(created.venue()).isEqualTo("Auditorio");
		assertThat(created.price()).isEqualByComparingTo(new BigDecimal("30.00"));
		assertThat(created.capacity()).isEqualTo(500);
		assertThat(created.reservedTickets()).isZero();
		assertThat(repository.findById(created.id())).contains(created);
	}

	@Test
	void updateReplacesEveryEditableFieldAndKeepsReservations() {
		repository.save(event(7L, "El Avaro de Molière", 400, 120));
		EventCommand command = command("El Avaro de Molière (nueva versión)", new BigDecimal("32.00"), 450);

		Event updated = service.update(7L, command);

		assertThat(updated.id()).isEqualTo(7L);
		assertThat(updated.name()).isEqualTo("El Avaro de Molière (nueva versión)");
		assertThat(updated.price()).isEqualByComparingTo(new BigDecimal("32.00"));
		assertThat(updated.capacity()).isEqualTo(450);
		assertThat(updated.reservedTickets()).isEqualTo(120);
	}

	@Test
	void updateThrowsEventNotFoundExceptionWhenItDoesNotExist() {
		assertThatThrownBy(() -> service.update(999L, command("No existe", new BigDecimal("10.00"), 100)))
			.isInstanceOf(EventNotFoundException.class)
			.hasMessageContaining("999");
	}

	@Test
	void updateRejectsCapacityBelowCurrentReservations() {
		repository.save(event(7L, "El Avaro de Molière", 400, 120));

		assertThatThrownBy(() -> service.update(7L, command("El Avaro", new BigDecimal("32.00"), 100)))
			.isInstanceOf(EventConflictException.class)
			.hasMessageContaining("120");
	}

	@Test
	void deleteRemovesAnEventWithoutReservations() {
		repository.save(event(9L, "Taller sin reservas", 300, 0));

		service.delete(9L);

		assertThat(repository.findById(9L)).isEmpty();
	}

	@Test
	void deleteThrowsEventNotFoundExceptionWhenItDoesNotExist() {
		assertThatThrownBy(() -> service.delete(999L))
			.isInstanceOf(EventNotFoundException.class)
			.hasMessageContaining("999");
	}

	@Test
	void deleteRejectsAnEventWithReservedTickets() {
		repository.save(event(4L, "Evento agotado", 15400, 15400));

		assertThatThrownBy(() -> service.delete(4L))
			.isInstanceOf(EventConflictException.class)
			.hasMessageContaining("15400");
	}

	@Test
	void createPublishesAnEventCreatedWithTheCreatedSnapshot() {
		EventCommand command = command("Noche de Jazz a la Fresca", new BigDecimal("30.00"), 500);

		Event created = service.create(command);

		assertThat(publisher.published()).hasSize(1);
		assertThat(publisher.published().getFirst()).isInstanceOf(EventCreated.class);
		EventCreated event = (EventCreated) publisher.published().getFirst();
		assertThat(event.snapshot()).isEqualTo(EventSnapshot.from(created));
		assertThat(event.snapshot().available()).isEqualTo(500);
	}

	@Test
	void updatePublishesAnEventUpdatedWithTheUpdatedSnapshot() {
		repository.save(event(7L, "El Avaro de Molière", 400, 120));
		EventCommand command = command("El Avaro de Molière (nueva versión)", new BigDecimal("32.00"), 450);

		Event updated = service.update(7L, command);

		assertThat(publisher.published()).hasSize(1);
		assertThat(publisher.published().getFirst()).isInstanceOf(EventUpdated.class);
		EventUpdated event = (EventUpdated) publisher.published().getFirst();
		assertThat(event.snapshot()).isEqualTo(EventSnapshot.from(updated));
		assertThat(event.snapshot().available()).isEqualTo(330);
	}

	@Test
	void failedUpdatesDoNotPublishAnything() {
		repository.save(event(7L, "El Avaro de Molière", 400, 120));

		assertThatThrownBy(() -> service.update(7L, command("El Avaro", new BigDecimal("32.00"), 100)))
			.isInstanceOf(EventConflictException.class);

		assertThat(publisher.published()).isEmpty();
	}

	@Test
	void deleteDoesNotPublishAnything() {
		repository.save(event(9L, "Taller sin reservas", 300, 0));

		service.delete(9L);

		assertThat(publisher.published()).isEmpty();
	}

	private static EventCommand command(String name, BigDecimal price, int capacity) {
		return new EventCommand(name, "Una descripción", EventCategory.CONCIERTO, "Auditorio",
			LocalDateTime.of(2026, 12, 31, 21, 0), price, capacity);
	}

	private static Event event(long id, String name, int capacity, int reservedTickets) {
		return new Event(id, name, "Una descripción", EventCategory.CONCIERTO, "Auditorio",
			LocalDateTime.of(2026, 12, 31, 21, 0), new BigDecimal("45.00"), capacity, reservedTickets);
	}

	private static final class FakeRepository implements EventRepository {

		private final Map<Long, Event> store = new HashMap<>();
		private long nextId = 1L;

		@Override
		public Page<Event> search(EventFilter filter, Pageable pageable) {
			return new PageImpl<>(List.copyOf(store.values()));
		}

		@Override
		public Optional<Event> findById(Long id) {
			return Optional.ofNullable(store.get(id));
		}

		@Override
		public Event save(Event event) {
			Event saved = event.id() == null
				? new Event(nextId++, event.name(), event.description(), event.category(), event.venue(),
					event.startsAt(), event.price(), event.capacity(), event.reservedTickets())
				: event;
			store.put(saved.id(), saved);
			return saved;
		}

		@Override
		public void deleteById(Long id) {
			store.remove(id);
		}

		@Override
		public void updateReservedTickets(Long eventId, int reservedTickets) {
			Event existing = store.get(eventId);
			if (existing != null) {
				store.put(eventId, new Event(existing.id(), existing.name(), existing.description(),
					existing.category(), existing.venue(), existing.startsAt(), existing.price(),
					existing.capacity(), reservedTickets));
			}
		}
	}

	private static final class FakePublisher implements EventPublisher {

		private final List<CatalogEvent> published = new ArrayList<>();

		@Override
		public void publish(CatalogEvent event) {
			published.add(event);
		}

		List<CatalogEvent> published() {
			return List.copyOf(published);
		}
	}
}
