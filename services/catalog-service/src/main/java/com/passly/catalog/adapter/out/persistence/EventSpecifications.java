package com.passly.catalog.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.passly.catalog.domain.EventCategory;
import com.passly.catalog.domain.EventFilter;
import org.springframework.data.jpa.domain.Specification;

/**
 * Convierte un {@link EventFilter} del dominio en una {@link Specification} de JPA.
 * Todo el conocimiento de Criteria API queda aislado aquí.
 */
public final class EventSpecifications {

	private static final char ESCAPE_CHAR = '\\';

	private EventSpecifications() {
	}

	public static Specification<EventJpaEntity> from(EventFilter filter) {
		List<Specification<EventJpaEntity>> specs = new ArrayList<>();
		if (filter.hasText()) {
			specs.add(matchesText(filter.text()));
		}
		if (filter.hasCategory()) {
			specs.add(matchesCategory(filter.category()));
		}
		if (filter.hasDate()) {
			specs.add(matchesDate(filter.date()));
		}
		if (filter.hasVenue()) {
			specs.add(matchesVenue(filter.venue()));
		}
		return Specification.allOf(specs.toArray(Specification[]::new));
	}

	private static Specification<EventJpaEntity> matchesText(String text) {
		String pattern = likePattern(text);
		return (root, query, cb) -> cb.or(
			cb.like(cb.lower(root.get("name")), pattern, ESCAPE_CHAR),
			cb.like(cb.lower(root.get("description")), pattern, ESCAPE_CHAR));
	}

	private static Specification<EventJpaEntity> matchesCategory(EventCategory category) {
		return (root, query, cb) -> cb.equal(root.get("category"), category);
	}

	private static Specification<EventJpaEntity> matchesDate(LocalDate date) {
		return (root, query, cb) -> {
			LocalDateTime from = date.atStartOfDay();
			LocalDateTime to = date.plusDays(1).atStartOfDay();
			return cb.between(root.get("startsAt"), from, to);
		};
	}

	private static Specification<EventJpaEntity> matchesVenue(String venue) {
		return (root, query, cb) -> cb.like(cb.lower(root.get("venue")), likePattern(venue), ESCAPE_CHAR);
	}

	private static String likePattern(String raw) {
		String escaped = raw.trim()
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_")
			.toLowerCase();
		return "%" + escaped + "%";
	}
}
