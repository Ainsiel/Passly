package com.passly.catalog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventJpaRepository extends JpaRepository<EventJpaEntity, Long>, JpaSpecificationExecutor<EventJpaEntity> {
}
