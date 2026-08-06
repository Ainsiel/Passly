package com.passly.booking.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad JPA de la tabla {@code tickets}. Cada Ticket pertenece a una
 * {@link ReservationJpaEntity} y lleva un código único global.
 */
@Entity
@Table(name = "tickets")
public class TicketJpaEntity {

	@Id
	@Column(columnDefinition = "uuid")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reservation_id", nullable = false)
	private ReservationJpaEntity reservation;

	@Column(nullable = false, length = 32, unique = true)
	private String code;

	@Column(nullable = false, length = 255)
	private String qr;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public TicketJpaEntity() {
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public ReservationJpaEntity getReservation() {
		return reservation;
	}

	public void setReservation(ReservationJpaEntity reservation) {
		this.reservation = reservation;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getQr() {
		return qr;
	}

	public void setQr(String qr) {
		this.qr = qr;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
