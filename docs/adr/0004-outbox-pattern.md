# Outbox pattern con poller y RabbitMQ

La Reserva y su evento de notificación se escriben en la misma transacción (tabla `outbox`), de modo que ningún Ticket se pierde entre el commit de la Reserva y el envío del email. Un poller publica cada ~2 segundos los mensajes pendientes a RabbitMQ y los marca como enviados. Se usa `spring-amqp` (`@RabbitListener` + `RabbitTemplate`), no Spring Cloud Stream, porque la abstracción de binder solo paga en pipelines multi-broker. Se descartó CDC con Debezium por su complejidad operativa; el polling es suficiente a este volumen.
