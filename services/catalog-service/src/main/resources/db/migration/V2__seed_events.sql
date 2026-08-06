-- Seed de 30 eventos variados para cubrir todos los filtros del catálogo
-- (texto, categoría, fecha, lugar) y estados de Disponibilidad (incl. agotado).

INSERT INTO events (id, name, description, category, venue, starts_at, price, capacity, reserved_tickets) VALUES
    -- CONCIERTO
    (1,  'Concierto de la Orquesta Sinfónica de Madrid', 'Noche de obras maestras sinfónicas dirigidas por la titular.', 'CONCIERTO', 'Auditorio Nacional', '2026-09-12 20:00', 45.00, 2200, 1900),
    (2,  'Bad Bunny: World Tour', 'Concierto multitudinario del artista del momento.', 'CONCIERTO', 'Estadio Metropolitano', '2026-10-03 21:00', 85.00, 60000, 48000),
    (3,  'Jazz en el Patio', 'Velada de jazz a la fresca en un entorno íntimo.', 'CONCIERTO', 'Teatro Real', '2026-08-20 21:30', 30.00, 800, 300),
    (4,  'Rock Alternativo: Vetusta Morla', 'Gira de regreso con su último disco.', 'CONCIERTO', 'Wizink Center', '2026-11-07 21:00', 38.00, 15400, 15400),
    (5,  'Música Electrónica: Solomun', 'Sesión de techno para despedir el año.', 'CONCIERTO', 'Sala Riviera', '2026-12-31 23:00', 55.00, 2500, 2500),
    -- TEATRO
    (6,  'La Casa de Bernarda Alba', 'El clásico de Lorca sobre la represión y el honor.', 'TEATRO', 'Teatro Calderón', '2026-09-12 19:00', 28.00, 900, 700),
    (7,  'El Avaro de Molière', 'Comedia ácida sobre la avaricia, en versión actualizada.', 'TEATRO', 'Teatro Lara', '2026-08-20 18:00', 25.00, 400, 120),
    (8,  'Don Juan Tenorio', 'El mito romántico en una puesta en escena renovada.', 'TEATRO', 'Teatro Real', '2026-10-30 20:00', 35.00, 1500, 800),
    (9,  'Improvisación Teatral en Familia', 'Espectáculo participativo de comedia improvisada para todas las edades.', 'TEATRO', 'Matadero Madrid', '2026-11-15 17:00', 12.00, 300, 90),
    -- DEPORTE
    (10, 'Final de la Copa del Rey', 'La gran final del fútbol español en el Metropolitano.', 'DEPORTE', 'Estadio Metropolitano', '2026-09-19 21:00', 120.00, 67000, 61000),
    (11, 'Derbi Madrileño: Atlético vs Real', 'El clásico de la capital decide al líder de la liga.', 'DEPORTE', 'Estadio Metropolitano', '2026-10-18 18:00', 95.00, 67000, 54000),
    (12, 'Maratón de Madrid', '42 kilómetros por los puntos más emblemáticos de la ciudad.', 'DEPORTE', 'Paseo del Prado', '2026-11-22 08:00', 40.00, 35000, 20000),
    (13, 'Open de Tenis: Semifinales', 'Las cuatro mejores raquetas del circuito se citan en la arcilla.', 'DEPORTE', 'Caja Mágica', '2026-09-12 15:00', 50.00, 12500, 9000),
    (14, 'Partido de la Liga Endesa', 'Choque de baloncesto entre los dos grandes del norte.', 'DEPORTE', 'WiZink Center', '2026-10-24 20:30', 35.00, 15400, 7500),
    -- FESTIVAL
    (15, 'Festival Sonora Primavera', 'Tres escenarios y cuarenta artistas en un solo recinto.', 'FESTIVAL', 'Recinto Ferial IFEMA', '2026-09-12 12:00', 70.00, 40000, 40000),
    (16, 'Festival de Verano en el Retiro', 'Música, teatro y gastronomía en el corazón del parque.', 'FESTIVAL', 'Parque del Retiro', '2026-08-20 18:00', 20.00, 8000, 5000),
    (17, 'Sónar Barcelona', 'El festival de música electrónica más importante de Europa.', 'FESTIVAL', 'Fira Barcelona', '2026-10-09 12:00', 110.00, 80000, 62000),
    (18, 'Festival Flamenco en el Sur', 'Noches de cante y baile flamenco bajo las estrellas.', 'FESTIVAL', 'Teatro Romano de Mérida', '2026-11-06 21:00', 45.00, 3000, 2100),
    -- CONFERENCIA
    (19, 'Conferencia Anual de Tecnología', 'Dos días de ponencias sobre el futuro del software.', 'CONFERENCIA', 'IFEMA Auditorio', '2026-10-14 09:00', 150.00, 5000, 4800),
    (20, 'Keynote: Inteligencia Artificial y Derechos', 'Debate abierto sobre la regulación de la IA.', 'CONFERENCIA', 'Pabellón de Cristal', '2026-09-24 10:00', 0.00, 1200, 1100),
    (21, 'Charla de Emprendimiento para Jóvenes', 'Casos reales de fundadores que empezaron en el garaje.', 'CONFERENCIA', 'Impact Hub Madrid', '2026-11-18 17:30', 5.00, 200, 60),
    -- EXPOSICION
    (22, 'Exposición: El Bosque Sumergido', 'Instalación inmersiva sobre los bosques bajo el mar.', 'EXPOSICION', 'CaixaForum', '2026-09-05 10:00', 8.00, 600, 250),
    (23, 'Exposición de Arte Contemporáneo', 'Recorrido por las vanguardias del último siglo.', 'EXPOSICION', 'Museo Reina Sofía', '2026-08-20 10:00', 12.00, 2000, 900),
    (24, 'Exposición: Cuerpo Humano', 'Cuerpos reales plastinados para entender la anatomía.', 'EXPOSICION', 'IFEMA', '2026-10-20 10:00', 15.00, 3000, 3000),
    (25, 'Muestra de Fotografía Urbana', 'Miradas nocturnas a las ciudades desde el objetivo.', 'EXPOSICION', 'Matadero Madrid', '2026-12-05 11:00', 6.00, 500, 130),
    -- CINE
    (26, 'Ciclo de Cine Clásico: Metrópolis', 'Proyección restaurada del clásico de Fritz Lang.', 'CINE', 'Cine Capitol', '2026-09-15 19:00', 9.00, 900, 500),
    (27, 'Estreno: Cine de Autor Latinoamericano', 'La nueva generación del cine del continente.', 'CINE', 'Cineteca Madrid', '2026-10-25 20:00', 7.50, 400, 400),
    (28, 'Cine al Aire Libre en el Patio', 'Proyección nocturna con entrada hasta completar aforo.', 'CINE', 'Matadero Madrid', '2026-08-22 22:00', 10.00, 600, 350),
    -- TALLER
    (29, 'Taller de Cerámica para Niños', 'Los más pequeños modelan su primera pieza.', 'TALLER', 'Casa de la Cultura', '2026-11-08 11:00', 15.00, 40, 40),
    (30, 'Taller de Cocina Saludable en Familia', 'Recetas fáciles y nutritivas para cocinar en casa.', 'TALLER', 'Mercado de San Antón', '2026-09-26 12:00', 25.00, 50, 18);

SELECT setval('events_id_seq', (SELECT MAX(id) FROM events));
