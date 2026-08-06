package com.passly.booking.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.passly.booking.application.port.EventProjectionRepository;
import com.passly.booking.domain.EventProjection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventProjectionServiceTest {

	private final FakeRepository repository = new FakeRepository();
	private final EventProjectionService service = new EventProjectionService(repository);

	@Test
	void upsertDelegatesToTheRepository() {
		EventProjection projection = new EventProjection(7L, "Noche de Jazz",
			LocalDateTime.of(2026, 12, 31, 21, 0), new BigDecimal("30.00"), 500, 120);

		service.upsert(projection);

		assertThat(repository.upserted).containsExactly(projection);
	}

	private static final class FakeRepository implements EventProjectionRepository {

		private final List<EventProjection> upserted = new ArrayList<>();

		@Override
		public void upsert(EventProjection projection) {
			upserted.add(projection);
		}
	}
}
