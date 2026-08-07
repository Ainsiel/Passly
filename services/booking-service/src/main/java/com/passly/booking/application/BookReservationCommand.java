package com.passly.booking.application;

/**
 * Comando para reservar Tickets de un Evento. {@code email} es el destinatario
 * de los Tickets (se entregan por email, ticket #8). La validación de forma
 * ocurre en el borde web (Bean Validation); las invariantes de la Reserva y la
 * Disponibilidad se validan en el dominio.
 */
public record BookReservationCommand(Long eventId, int quantity, String email) {
}
