package com.passly.booking.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.passly.booking.domain.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, UUID> {

	@EntityGraph(attributePaths = "tickets")
	Optional<ReservationJpaEntity> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

	boolean existsByUserIdAndEventIdAndStatus(String userId, Long eventId, ReservationStatus status);

	@EntityGraph(attributePaths = "tickets")
	List<ReservationJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
