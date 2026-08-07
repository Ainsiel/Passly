-- El email destinatario de los Tickets (ticket #8). Default vacío solo para
-- migrar filas existentes del entorno develop; el dominio exige email no blanco
-- y el borde web lo valida con @Email.
ALTER TABLE reservations ADD COLUMN email VARCHAR(255) NOT NULL DEFAULT '';
