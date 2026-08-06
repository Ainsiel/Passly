package com.passly.catalog.adapter.in.web;

import java.time.LocalDate;

import com.passly.catalog.adapter.in.web.dto.EventDetailResponse;
import com.passly.catalog.adapter.in.web.dto.EventPageResponse;
import com.passly.catalog.application.EventQueryService;
import com.passly.catalog.domain.EventCategory;
import com.passly.catalog.domain.EventFilter;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/events")
public class EventController {

	private final EventQueryService queryService;

	public EventController(EventQueryService queryService) {
		this.queryService = queryService;
	}

	@GetMapping
	public EventPageResponse list(
			@RequestParam(required = false) @Size(max = 100) String q,
			@RequestParam(required = false) EventCategory category,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) @Size(max = 100) String venue,
			@PageableDefault(size = 20, sort = "startsAt") Pageable pageable) {
		EventFilter filter = new EventFilter(q, category, date, venue);
		return EventPageResponse.from(queryService.search(filter, pageable));
	}

	@GetMapping("/{id}")
	public EventDetailResponse getById(@PathVariable Long id) {
		return EventDetailResponse.from(queryService.getById(id));
	}
}
