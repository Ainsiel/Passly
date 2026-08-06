package com.passly.catalog.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.passly.catalog.domain.EventCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA de la tabla {@code events}. Es un detalle de infraestructura:
 * el dominio nunca la ve (el adaptador mapea a {@code com.passly.catalog.domain.Event}).
 */
@Entity
@Table(name = "events")
public class EventJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String name;

	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private EventCategory category;

	@Column(nullable = false, length = 150)
	private String venue;

	@Column(nullable = false)
	private LocalDateTime startsAt;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private int capacity;

	@Column(name = "reserved_tickets", nullable = false)
	private int reservedTickets;

	protected EventJpaEntity() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public EventCategory getCategory() {
		return category;
	}

	public void setCategory(EventCategory category) {
		this.category = category;
	}

	public String getVenue() {
		return venue;
	}

	public void setVenue(String venue) {
		this.venue = venue;
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
}
