package com.passly.catalog.adapter.in.web;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.passly.catalog.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(EventApiIntegrationTest.JwtDecoderStubConfiguration.class)
class EventApiIntegrationTest extends AbstractPostgresIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void listEventsIsPublicAndReturnsPagedSeed() throws Exception {
		mockMvc.perform(get("/events"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.content", hasSize(20)))
			.andExpect(jsonPath("$.totalElements").value(30))
			.andExpect(jsonPath("$.totalPages").value(2))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.first").value(true))
			.andExpect(jsonPath("$.last").value(false))
			.andExpect(jsonPath("$.content[0].available").isNumber());
	}

	@Test
	void listEventsSupportsPaging() throws Exception {
		mockMvc.perform(get("/events").param("page", "1").param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(10)))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(10))
			.andExpect(jsonPath("$.totalElements").value(30))
			.andExpect(jsonPath("$.totalPages").value(3))
			.andExpect(jsonPath("$.first").value(false))
			.andExpect(jsonPath("$.last").value(false));
	}

	@Test
	void listEventsFiltersByCategory() throws Exception {
		mockMvc.perform(get("/events").param("category", "CONCIERTO"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(5))
			.andExpect(jsonPath("$.content[*].category", everyItem(is("CONCIERTO"))));

		mockMvc.perform(get("/events").param("category", "TALLER"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void listEventsFiltersByTextInNameAndDescription() throws Exception {
		mockMvc.perform(get("/events").param("q", "familia"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2));

		mockMvc.perform(get("/events").param("q", "sinfónica"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void listEventsFiltersByDate() throws Exception {
		mockMvc.perform(get("/events").param("date", "2026-08-20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(4));
	}

	@Test
	void listEventsFiltersByVenueCaseInsensitively() throws Exception {
		mockMvc.perform(get("/events").param("venue", "metropolitano"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(3));

		mockMvc.perform(get("/events").param("venue", "Estadio Metropolitano"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(3));
	}

	@Test
	void listEventsCombinesFilters() throws Exception {
		mockMvc.perform(get("/events")
				.param("category", "FESTIVAL")
				.param("date", "2026-09-12"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].name").value("Festival Sonora Primavera"));
	}

	@Test
	void listEventsWithNoMatchesReturnsEmptyPage() throws Exception {
		mockMvc.perform(get("/events").param("q", "zzzz-no-existe"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(0)))
			.andExpect(jsonPath("$.totalElements").value(0))
			.andExpect(jsonPath("$.first").value(true))
			.andExpect(jsonPath("$.last").value(true));
	}

	@Test
	void getEventDetailReturnsAllFields() throws Exception {
		mockMvc.perform(get("/events/16"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(16))
			.andExpect(jsonPath("$.name").value("Festival de Verano en el Retiro"))
			.andExpect(jsonPath("$.category").value("FESTIVAL"))
			.andExpect(jsonPath("$.venue").value("Parque del Retiro"))
			.andExpect(jsonPath("$.startsAt").value("2026-08-20T18:00:00"))
			.andExpect(jsonPath("$.price").value(20.0))
			.andExpect(jsonPath("$.capacity").value(8000))
			.andExpect(jsonPath("$.available").value(3000))
			.andExpect(jsonPath("$.description").isNotEmpty());
	}

	@Test
	void getEventDetailForSoldOutEventReturnsZeroAvailability() throws Exception {
		mockMvc.perform(get("/events/4"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.capacity").value(15400))
			.andExpect(jsonPath("$.available").value(0));
	}

	@Test
	void getEventDetailNotFoundReturnsProblemJson404() throws Exception {
		mockMvc.perform(get("/events/999"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("urn:problem-type:event-not-found"))
			.andExpect(jsonPath("$.title").value("Evento no encontrado"))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("No existe un evento con id 999"));
	}

	@Test
	void invalidCategoryParamReturnsProblemJson400() throws Exception {
		mockMvc.perform(get("/events").param("category", "NOEXISTE"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("urn:problem-type:bad-request"))
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void invalidDateParamReturnsProblemJson400() throws Exception {
		mockMvc.perform(get("/events").param("date", "20-08-2026"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void invalidSortParamReturnsProblemJson400() throws Exception {
		mockMvc.perform(get("/events").param("sort", "noexiste,asc"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class JwtDecoderStubConfiguration {

		@Bean
		@Primary
		JwtDecoder stubJwtDecoder() {
			return token -> null;
		}
	}
}
