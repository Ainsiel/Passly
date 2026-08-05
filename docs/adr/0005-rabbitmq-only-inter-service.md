# Comunicación inter-servicio únicamente por RabbitMQ

La única comunicación entre servicios es asíncrona vía RabbitMQ (Reservas → Notificaciones); no existe HTTP service-to-service. Esto elimina la necesidad de client-credentials, evita el acoplamiento temporal entre servicios y hace resiliente la entrega de emails (retries + DLQ en el consumidor). Cada servicio valida el JWT de Keycloak en su borde HTTP (resource-server) para el tráfico entrante de Usuarios.
