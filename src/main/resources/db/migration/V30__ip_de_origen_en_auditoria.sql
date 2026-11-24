-- La auditoría respondía "quién y cuándo", pero no "desde dónde". Sin la IP no se
-- distingue el acceso habitual de un operador del mismo acceso hecho desde otro sitio,
-- que es lo que permite detectar un abuso y no solo reconstruirlo después.
--
-- Queda nula en los registros anteriores y en los que no nacen de una petición: el
-- arranque que crea los usuarios iniciales, o un envío de correo asíncrono.
-- 45 caracteres es el máximo de una IPv6 escrita con zona.
alter table auth_audit
    add column ip_origen varchar(45);

-- Para responder "qué hizo esta IP" sin recorrer la tabla entera.
create index idx_auth_audit_ip_origen on auth_audit (ip_origen, ocurrido_en desc);
