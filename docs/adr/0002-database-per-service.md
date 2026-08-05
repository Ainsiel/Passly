# Database-per-service

Cada servicio es dueño de su base de datos (patrón database-per-service): un único contenedor Postgres aloja tres bases (`catalog`, `booking`, `notification`), y Flyway versiona los esquemas. Se eligió el patrón para que los bounded contexts no compartan esquema ni migraciones; se usó un solo contenedor para no sobre-operar el entorno local. La contrapartida —compartir la infraestructura de un contenedor— es aceptable porque en QA/desarrollo el aislamiento físico no aporta nada.
