package com.passly.catalog.adapter.in.web;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
import org.springframework.security.oauth2.jwt.Jwt;
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

	@Test
	void adminCanCreateReadUpdateAndDeleteAnEvent() throws Exception {
		String createBody = """
			{"name":"Noche de Jazz a la Fresca","description":"Velada de jazz íntima",
			"category":"CONCIERTO","venue":"Auditorio","startsAt":"2026-12-31T21:00:00",
			"price":30.00,"capacity":500}
			""";

		String location = mockMvc.perform(post("/events")
				.header("Authorization", "Bearer admin")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createBody))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", containsString("/events/")))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.name").value("Noche de Jazz a la Fresca"))
			.andExpect(jsonPath("$.category").value("CONCIERTO"))
			.andExpect(jsonPath("$.capacity").value(500))
			.andExpect(jsonPath("$.available").value(500))
			.andReturn().getResponse().getHeader("Location");

		mockMvc.perform(get(location))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Noche de Jazz a la Fresca"));

		String updateBody = """
			{"name":"Noche de Jazz a la Fresca (2027)","description":"Velada de jazz íntima",
			"category":"CONCIERTO","venue":"Auditorio Nacional","startsAt":"2027-01-01T21:00:00",
			"price":35.00,"capacity":600}
			""";

		mockMvc.perform(put(location)
				.header("Authorization", "Bearer admin")
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Noche de Jazz a la Fresca (2027)"))
			.andExpect(jsonPath("$.venue").value("Auditorio Nacional"))
			.andExpect(jsonPath("$.price").value(35.0))
			.andExpect(jsonPath("$.capacity").value(600))
			.andExpect(jsonPath("$.available").value(600));

		mockMvc.perform(get(location))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Noche de Jazz a la Fresca (2027)"))
			.andExpect(jsonPath("$.available").value(600));

		mockMvc.perform(delete(location).header("Authorization", "Bearer admin"))
			.andExpect(status().isNoContent());

		mockMvc.perform(get(location))
			.andExpect(status().isNotFound());
	}

	@Test
	void adminCannotDeleteAnEventWithReservedTickets() throws Exception {
		mockMvc.perform(delete("/events/4").header("Authorization", "Bearer admin"))
			.andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value(containsString("15400")));
	}

	@Test
	void adminCannotLowerCapacityBelowCurrentReservations() throws Exception {
		String updateBody = """
			{"name":"Rock Alternativo: Vetusta Morla","description":"Gira de regreso",
			"category":"CONCIERTO","venue":"Wizink Center","startsAt":"2026-11-07T21:00:00",
			"price":38.00,"capacity":100}
			""";

		mockMvc.perform(put("/events/4")
				.header("Authorization", "Bearer admin")
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateBody))
			.andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value(containsString("15400")));
	}

	@Test
	void updateAndDeleteOfNonExistentEventReturnProblemJson404() throws Exception {
		String updateBody = """
			{"name":"No existe","description":null,"category":"TEATRO","venue":"Sala",
			"startsAt":"2026-12-31T21:00:00","price":10.00,"capacity":100}
			""";

		mockMvc.perform(put("/events/999")
				.header("Authorization", "Bearer admin")
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateBody))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404));

		mockMvc.perform(delete("/events/999").header("Authorization", "Bearer admin"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void invalidCreateBodyReturns400WithFieldDetails() throws Exception {
		String invalidBody = """
			{"name":"","description":null,"category":"CONCIERTO","venue":"",
			"startsAt":null,"price":-5.00,"capacity":-1}
			""";

		mockMvc.perform(post("/events")
				.header("Authorization", "Bearer admin")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidBody))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("urn:problem-type:validation-error"))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors").isArray())
			.andExpect(jsonPath("$.errors", hasSize(5)))
			.andExpect(jsonPath("$.errors[*].field",
				containsInAnyOrder("name", "venue", "startsAt", "price", "capacity")))
			.andExpect(jsonPath("$.errors[*].message", everyItem(not(emptyString()))));
	}

	@Test
	void invalidCategoryValueReturns400() throws Exception {
		String invalidBody = """
			{"name":"Concierto","description":null,"category":"NOEXISTE","venue":"Sala",
			"startsAt":"2026-12-31T21:00:00","price":20.00,"capacity":100}
			""";

		mockMvc.perform(post("/events")
				.header("Authorization", "Bearer admin")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidBody))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("urn:problem-type:bad-request"))
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void priceWithMoreThanEightIntegerDigitsReturns400() throws Exception {
		String invalidBody = """
			{"name":"Concierto","description":null,"category":"CONCIERTO","venue":"Sala",
			"startsAt":"2026-12-31T21:00:00","price":123456789.00,"capacity":100}
			""";

		mockMvc.perform(post("/events")
				.header("Authorization", "Bearer admin")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("price")));
	}

	@Test
	void createWithoutAdminRoleReturns403() throws Exception {
		String validBody = """
			{"name":"Concierto de prueba","description":null,"category":"CONCIERTO","venue":"Sala",
			"startsAt":"2026-12-31T21:00:00","price":20.00,"capacity":100}
			""";

		mockMvc.perform(post("/events")
				.header("Authorization", "Bearer user")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBody))
			.andExpect(status().isForbidden());

		mockMvc.perform(put("/events/1")
				.header("Authorization", "Bearer user")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBody))
			.andExpect(status().isForbidden());

		mockMvc.perform(delete("/events/1").header("Authorization", "Bearer user"))
			.andExpect(status().isForbidden());
	}

	@Test
	void anonymousWriteRequestIsRejectedWith401() throws Exception {
		String validBody = """
			{"name":"Concierto de prueba","description":null,"category":"CONCIERTO","venue":"Sala",
			"startsAt":"2026-12-31T21:00:00","price":20.00,"capacity":100}
			""";

		mockMvc.perform(post("/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBody))
			.andExpect(status().isUnauthorized());
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class JwtDecoderStubConfiguration {

		@Bean
		@Primary
		JwtDecoder stubJwtDecoder() {
			Jwt admin = jwtWithRoles("ADMIN", "USER");
			Jwt user = jwtWithRoles("USER");
			return token -> switch (token) {
				case "admin" -> admin;
				case "user" -> user;
				default -> null;
			};
		}

		private static Jwt jwtWithRoles(String... roles) {
			return Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject("subject")
				.claim("preferred_username", "subject")
				.claim("realm_access", Map.of("roles", List.of(roles)))
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(300))
				.build();
		}
	}
}
