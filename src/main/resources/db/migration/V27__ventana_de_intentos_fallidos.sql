-- HU-09. Bloquear cuentas tras intentos fallidos de acceso.
--
-- El contador de intentos ya existía (V25), pero sumaba fallos consecutivos sin
-- mirar cuándo ocurrieron: dos errores de tecleo en enero y tres en marzo bloqueaban
-- la cuenta igual que cinco seguidos en un minuto. La historia pide cinco fallos
-- "en 15 minutos", así que hace falta saber cuándo fue el último.
alter table auth_users add column last_failed_at timestamp(6) with time zone;
