package com.passly.booking.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventProjectionJpaRepository extends JpaRepository<EventProjectionJpaEntity, Long> {

	/**
	 * Upsert atómico e idempotente: inserta la instantánea o la sustituye si el
	 * evento ya existe. {@code version} se incrementa en cada escritura para
	 * preservar el optimistic locking de la Reserva (ticket #7); el reenvío de
	 * un mensaje duplicado deja el mismo estado final salvo el contador.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
		INSERT INTO event_projections
			(event_id, name, starts_at, price, capacity, reserved_tickets, version, updated_at)
		VALUES (:eventId, :name, :startsAt, :price, :capacity, :reservedTickets, 0, now())
		ON CONFLICT (event_id) DO UPDATE SET
			name = EXCLUDED.name,
			starts_at = EXCLUDED.starts_at,
			price = EXCLUDED.price,
			capacity = EXCLUDED.capacity,
			reserved_tickets = EXCLUDED.reserved_tickets,
			version = event_projections.version + 1,
			updated_at = now()
		""", nativeQuery = true)
	void upsert(@Param("eventId") Long eventId, @Param("name") String name,
			@Param("startsAt") LocalDateTime startsAt, @Param("price") BigDecimal price,
			@Param("capacity") int capacity, @Param("reservedTickets") int reservedTickets);
}
