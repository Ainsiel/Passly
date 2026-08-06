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

	static EventJpaEntity toEntity(Event event) {
		EventJpaEntity entity = new EventJpaEntity();
		entity.setId(event.id());
		entity.setName(event.name());
		entity.setDescription(event.description());
		entity.setCategory(event.category());
		entity.setVenue(event.venue());
		entity.setStartsAt(event.startsAt());
		entity.setPrice(event.price());
		entity.setCapacity(event.capacity());
		entity.setReservedTickets(event.reservedTickets());
		return entity;
	}
}
