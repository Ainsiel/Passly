package com.passly.booking.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Entidad JPA de la tabla {@code event_projections}. {@code version} (columna
 * {@code @Version}) queda lista para el optimistic locking de la Reserva
 * (ticket #7): cada escritura de la proyección incrementa la versión y una
 * reserva concurrente sobre una versión caduca falla y se reintenta.
 */
@Entity
@Table(name = "event_projections")
public class EventProjectionJpaEntity {

	@Id
	@Column(name = "event_id")
	private Long eventId;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(name = "starts_at", nullable = false)
	private LocalDateTime startsAt;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private int capacity;

	@Column(name = "reserved_tickets", nullable = false)
	private int reservedTickets;

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public EventProjectionJpaEntity() {
	}

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDateTime getStartsAt() {
		return startsAt;
	}

	public void setStartsAt(LocalDateTime startsAt) {
		this.startsAt = startsAt;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getReservedTickets() {
		return reservedTickets;
	}

	public void setReservedTickets(int reservedTickets) {
		this.reservedTickets = reservedTickets;
	}

	public long getVersion() {
		return version;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
