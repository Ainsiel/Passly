package com.passly.catalog.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventOutboxJpaRepository extends JpaRepository<EventOutboxJpaEntity, Long> {

	List<EventOutboxJpaEntity> findTop100ByPublishedAtIsNullOrderByIdAsc();
}
