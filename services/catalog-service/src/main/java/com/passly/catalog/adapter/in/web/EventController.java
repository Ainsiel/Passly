package com.passly.catalog.adapter.in.web;

import java.time.LocalDate;

import com.passly.catalog.adapter.in.web.dto.EventDetailResponse;
import com.passly.catalog.adapter.in.web.dto.EventPageResponse;
import com.passly.catalog.adapter.in.web.dto.EventUpsertRequest;
import com.passly.catalog.application.EventCommandService;
import com.passly.catalog.application.EventQueryService;
import com.passly.catalog.domain.Event;
import com.passly.catalog.domain.EventCategory;
import com.passly.catalog.domain.EventFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/events")
public class EventController {

	private final EventQueryService queryService;
	private final EventCommandService commandService;

	public EventController(EventQueryService queryService, EventCommandService commandService) {
		this.queryService = queryService;
		this.commandService = commandService;
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

	@PostMapping
	public ResponseEntity<EventDetailResponse> create(@Valid @RequestBody EventUpsertRequest request,
			UriComponentsBuilder uriBuilder) {
		Event created = commandService.create(request.toCommand());
		return ResponseEntity.created(uriBuilder.path("/events/{id}").buildAndExpand(created.id()).toUri())
			.body(EventDetailResponse.from(created));
	}

	@PutMapping("/{id}")
	public EventDetailResponse update(@PathVariable Long id, @Valid @RequestBody EventUpsertRequest request) {
		return EventDetailResponse.from(commandService.update(id, request.toCommand()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		commandService.delete(id);
	}
}
