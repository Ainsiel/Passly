# Microservicios con límites de contexto

Passly se construye como tres microservicios (Catálogo, Reservas, Notificaciones) más un gateway Spring Cloud Gateway. Se eligió esta frontera porque cada contexto tiene ciclo de vida, esquema de datos y tasa de cambio independientes, y porque el objetivo del proyecto es demostrar arquitectura de microservicios reales en un CV. Alternativas descartadas: monolito modular (no muestra las técnicas buscadas) y un servicio por caso de uso (fragmenta sin criterio).
