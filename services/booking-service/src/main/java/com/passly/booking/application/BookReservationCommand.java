package com.passly.booking.application;

/**
 * Comando para reservar Tickets de un Evento. La validación de forma ocurre
 * en el borde web (Bean Validation); las invariantes de la Reserva y la
 * Disponibilidad se validan en el dominio.
 */
public record BookReservationCommand(Long eventId, int quantity) {
}
