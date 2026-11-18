-- HU-16. Registrar una excepción logística.
--
-- Por qué un envío no pudo seguir su curso: destinatario ausente, dirección
-- incorrecta, paquete dañado, rechazado o retenido. Solo lo llevan los eventos de
-- tipo EXCEPTION; en los movimientos normales queda nulo, porque avanzar no
-- necesita explicación.
--
-- La tabla la escribe la API de logística, pero las migraciones se ejecutan aquí:
-- es la única de las tres que tiene Flyway habilitado contra la base compartida.
alter table logistics_events add column exception_reason varchar(30);
