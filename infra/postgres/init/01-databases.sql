-- Bases de datos del entorno develop (ADR-0002: database-per-service).
-- Se ejecutan solo en la primera inicialización del volumen de Postgres.
CREATE DATABASE catalog;
CREATE DATABASE booking;
CREATE DATABASE notification;
