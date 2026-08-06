package com.passly.booking.application.port;

/**
 * Puerto de salida que produce códigos únicos para los Tickets. El adaptador
 * genera valores aleatorios; los tests lo sustituyen por uno determinista.
 * La unicidad real la garantiza la columna {@code code} (UNIQUE) de la BD.
 */
public interface TicketCodeGenerator {

	String next();
}
