-- HU-10. Cambio y recuperación de contraseña.
--
-- Para recuperar una cuenta hace falta un canal por el que avisar a su dueño, y
-- hasta ahora el usuario no tenía ninguno: solo nombre y contraseña.

-- Opcional: los usuarios creados desde el entorno al arrancar no lo traen, y sin
-- correo simplemente no se puede recuperar la clave por este medio.
alter table auth_users add column email varchar(120);

-- Único cuando está presente: dos cuentas con el mismo correo harían ambiguo a
-- quién pertenece un enlace de recuperación. Postgres ignora los nulos en los
-- índices únicos, así que varias cuentas sin correo conviven sin chocar.
create unique index uk_auth_users_email on auth_users (lower(email)) where email is not null;

-- Enlaces de recuperación. No se guarda el token, solo su huella: si alguien
-- leyera esta tabla no podría usar ninguno de los enlaces pendientes.
create table auth_password_resets (
    token_hash varchar(64) primary key,
    username varchar(50) not null references auth_users (username),
    created_at timestamp(6) with time zone not null,
    expires_at timestamp(6) with time zone not null,
    -- Null mientras no se haya usado; marca el uso único.
    used_at timestamp(6) with time zone
);

create index idx_auth_password_resets_username on auth_password_resets (username);

-- Marca las cuentas cuya contraseña la gobierna su dueño. Los usuarios iniciales se
-- crean y se actualizan desde variables de entorno al arrancar; sin esta bandera, el
-- primer reinicio despues de un cambio de clave la devolveria al valor del entorno y
-- el cambio de HU-10 se perderia.
alter table auth_users add column password_set_by_user boolean not null default false;
