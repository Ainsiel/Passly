package com.passly.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.passly.catalog.application.port.EventRepository;
import com.passly.catalog.domain.Event;
import com.passly.catalog.domain.EventCategory;
import com.passly.catalog.domain.EventFilter;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class EventQueryServiceTest {

	private final Event sinfonica = event(1L, "Concierto sinfónica de primavera");
	private final Event jara = event(2L, "Recital de Joaquín Sabina");
	private final FakeRepository repository = new FakeRepository(List.of(sinfonica, jara));
	private final EventQueryService service = new EventQueryService(repository);

	@Test
	void searchDelegatesTheFilterAndPageableToTheRepository() {
		EventFilter filter = new EventFilter("sinfónica", EventCategory.CONCIERTO, LocalDateTime.of(2026, 9, 12, 20, 0).toLocalDate(), "Auditorio");
		Pageable pageable = PageRequest.of(2, 15);

		Page<Event> page = service.search(filter, pageable);

		assertThat(repository.lastFilter).isEqualTo(filter);
		assertThat(repository.lastPageable).isEqualTo(pageable);
		assertThat(page.getContent()).extracting(Event::name)
			.containsExactly("Concierto sinfónica de primavera", "Recital de Joaquín Sabina");
	}

	@Test
	void getByIdReturnsTheEventWhenItExists() {
		assertThat(service.getById(1L)).isEqualTo(sinfonica);
	}

	@Test
	void getByIdThrowsEventNotFoundExceptionWhenItDoesNotExist() {
		assertThatThrownBy(() -> service.getById(999L))
			.isInstanceOf(EventNotFoundException.class)
			.hasMessageContaining("999");
	}

	private static Event event(long id, String name) {
		return new Event(id, name, "Descripción", EventCategory.CONCIERTO, "Auditorio",
			LocalDateTime.of(2026, 9, 12, 20, 0), new BigDecimal("45.00"), 100, 30);
	}

	private static final class FakeRepository implements EventRepository {

		private final Page<Event> result;
		private final Map<Long, Event> byId = new HashMap<>();
		private EventFilter lastFilter;
		private Pageable lastPageable;

		private FakeRepository(List<Event> events) {
			this.result = new PageImpl<>(events);
			events.forEach(event -> byId.put(event.id(), event));
		}

		@Override
		public Page<Event> search(EventFilter filter, Pageable pageable) {
			this.lastFilter = filter;
			this.lastPageable = pageable;
			return result;
		}

		@Override
		public Optional<Event> findById(Long id) {
			return Optional.ofNullable(byId.get(id));
		}

		@Override
		public Event save(Event event) {
			byId.put(event.id(), event);
			return event;
		}

		@Override
		public void deleteById(Long id) {
			byId.remove(id);
		}

		@Override
		public void updateReservedTickets(Long eventId, int reservedTickets) {
			Event existing = byId.get(eventId);
			if (existing != null) {
				byId.put(eventId, new Event(existing.id(), existing.name(), existing.description(),
					existing.category(), existing.venue(), existing.startsAt(), existing.price(),
					existing.capacity(), reservedTickets));
			}
		}
	}
}
