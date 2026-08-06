# Passly — Contexto

Sistema de reserva de tickets para eventos. Un Usuario navega el Catálogo de Eventos, reserva tickets de un Evento y recibe sus Tickets por email.

## Language

### Dominio central

**Usuario**:
Persona que navega eventos, hace Reservas y recibe Tickets. Se autentica contra Keycloak.
_Avoid_: cliente, account, user

**Evento**:
Actividad u obra programada que los Usuarios pueden reservar. Tiene fecha, lugar, categoría, precio y Capacidad.
_Avoid_: show, act, función

**Capacidad**:
Número máximo de Tickets vendibles de un Evento.

**Disponibilidad**:
Capacidad menos Tickets vendidos. "Agotado" es Disponibilidad cero; no es un estado gestionable.
_Avoid_: stock, cupo

**Reserva**:
Agregado que vincula a un Usuario con un Evento y contiene uno o más Tickets. Máximo una Reserva activa por (Usuario, Evento).
_Avoid_: booking, order, compra, orden

**Ticket**:
Entidad emitida dentro de una Reserva, con código único y código QR. Se entrega por email.
_Avoid_: entrada, boleto, pass

**Proyección de Evento**:
Reflejo del Evento en el contexto Reservas, sincronizado desde Catálogo vía RabbitMQ (ADR-0011). Mantiene Capacidad y Disponibilidad para controlar la concurrencia de las Reservas; no es gestionable por el Usuario.
_Avoid_: evento duplicado

### Contextos

**Catálogo**:
Contexto que posee los Eventos y su CRUD, protegido por el rol `ADMIN`.

**Reservas**:
Contexto que posee las Reservas y los Tickets, y controla la concurrencia sobre la Capacidad. Mantiene la Proyección de Evento sincronizada desde Catálogo.

**Notificaciones**:
Contexto que entrega los Tickets por email.
