-- Los roles pasan a contenerse unos a otros (ver SecurityConfig): el administrador
-- puede lo del operador y lo del supervisor sin que haga falta darselos aparte.
--
-- Las cuentas creadas antes llevan los tres escritos, lo que ya no aporta nada y
-- ademas confunde: la pantalla de usuarios muestra tres roles donde basta uno. Se
-- dejan con ADMIN a secas.
--
-- Solo se tocan las que tienen ADMIN: a un supervisor que ademas sea operador no se
-- le quita nada, porque esos dos roles siguen siendo independientes entre si.
update auth_users
   set roles = 'ADMIN'
 where roles like '%ADMIN%'
   and roles <> 'ADMIN';
