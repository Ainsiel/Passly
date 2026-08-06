package com.passly.booking.application.port;

import java.util.List;
import java.util.Optional;

import com.passly.booking.domain.Reservation;

/**
 * Puerto de salida del agregado Reserva. La idempotency key se pasa aparte:
 * es un concern del request (reintentos del cliente), no del dominio; el
 * adaptador la persiste para detectar reenvíos.
 */
public interface ReservationRepository {

	Optional<Reservation> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

	boolean hasActiveReservation(String userId, Long eventId);

	Reservation save(Reservation reservation, String idempotencyKey);

	List<Reservation> findByUserIdOrderByCreatedAtDesc(String userId);
}
