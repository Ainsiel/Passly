package com.passly.catalog.adapter.out.persistence;

import com.passly.catalog.domain.Event;

final class EventMapper {

	private EventMapper() {
	}

	static Event toDomain(EventJpaEntity entity) {
		return new Event(entity.getId(), entity.getName(), entity.getDescription(), entity.getCategory(),
			entity.getVenue(), entity.getStartsAt(), entity.getPrice(), entity.getCapacity(),
			entity.getReservedTickets());
	}
}
