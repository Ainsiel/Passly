package com.passly.booking.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationOutboxJpaRepository extends JpaRepository<ReservationOutboxJpaEntity, Long> {

	List<ReservationOutboxJpaEntity> findTop100ByPublishedAtIsNullOrderByIdAsc();
}
