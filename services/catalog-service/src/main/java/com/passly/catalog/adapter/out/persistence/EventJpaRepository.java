package com.passly.catalog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventJpaRepository extends JpaRepository<EventJpaEntity, Long>, JpaSpecificationExecutor<EventJpaEntity> {

	@Modifying
	@Query("UPDATE EventJpaEntity e SET e.reservedTickets = :reservedTickets WHERE e.id = :eventId")
	void updateReservedTickets(@Param("eventId") Long eventId, @Param("reservedTickets") int reservedTickets);
}
