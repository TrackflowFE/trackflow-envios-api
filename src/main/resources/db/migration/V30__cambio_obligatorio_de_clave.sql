-- Cuando un administrador crea una cuenta, fija una contraseña temporal que tiene que
-- hacerle llegar a su dueño. Mientras esa clave siga sirviendo, hay una credencial
-- conocida por dos personas y viajando por donde se la hayan pasado.
--
-- Esta marca obliga a cambiarla antes de poder hacer nada: el ingreso funciona, pero
-- la sesión no sirve para nada más hasta que su dueño elija una clave propia.
alter table auth_users add column must_change_password boolean not null default false;
