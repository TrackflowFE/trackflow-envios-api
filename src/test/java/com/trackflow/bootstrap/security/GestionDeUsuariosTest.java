package com.trackflow.bootstrap.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class GestionDeUsuariosTest {

    private static final Instant AHORA = Instant.parse("2026-11-01T15:00:00Z");

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final Sesiones sesiones = mock(Sesiones.class);
    private final Auditorias auditorias = mock(Auditorias.class);
    private final PoliticaDeClaves politica = mock(PoliticaDeClaves.class);
    private final InvitacionDeCuenta invitacion = mock(InvitacionDeCuenta.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final GestionDeUsuarios gestion = new GestionDeUsuarios(usuarios, sesiones, auditorias, politica,
            invitacion, encoder, Clock.fixed(AHORA, ZoneOffset.UTC));

    private Usuario usuario(String nombre, String roles, boolean activo) {
        return new Usuario(nombre, "hash", roles, activo, AHORA);
    }

    private void existe(Usuario usuario) {
        when(usuarios.findById(usuario.getUsername())).thenReturn(Optional.of(usuario));
    }

    private static int estado(ResponseStatusException error) {
        return error.getStatusCode().value();
    }

    // --- listar ---

    @Test
    void listarDevuelveTodosLosUsuarios() {
        List<Usuario> todos = List.of(usuario("ana", "OPERADOR", true));
        when(usuarios.findAll()).thenReturn(todos);

        assertSame(todos, gestion.listar());
    }

    // --- crear ---

    @Test
    void crearConUnNombreYaUsadoDaConflicto() {
        when(usuarios.existsById("ana")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> gestion.crear("admin", "ana", "clave-larga-123", "OPERADOR", null));

        assertEquals(409, estado(error));
        verify(usuarios, never()).save(any());
    }

    @Test
    void crearSinRolesDaError() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> gestion.crear("admin", "ana", "clave-larga-123", "  ", null));

        assertEquals(400, estado(error));
        assertEquals("Indique al menos un rol", error.getReason());
    }

    @Test
    void crearConUnRolDesconocidoDaError() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> gestion.crear("admin", "ana", "clave-larga-123", "jefe", null));

        assertEquals(400, estado(error));
        assertTrue(error.getReason().contains("Rol desconocido: JEFE"));
    }

    @Test
    void crearConUnaClaveQueNoCumpleLaPoliticaNoGuardaNada() {
        org.mockito.Mockito.doThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, PoliticaDeClaves.MUY_CORTA))
                .when(politica).exigir("corta");

        assertThrows(ResponseStatusException.class,
                () -> gestion.crear("admin", "ana", "corta", "OPERADOR", null));

        verify(usuarios, never()).save(any());
    }

    @Test
    void crearSinCorreoNormalizaLosRolesYExigeCambiarLaClave() {
        when(encoder.encode("clave-larga-123")).thenReturn("hash-nuevo");

        Usuario creado = gestion.crear("admin", "ana", "clave-larga-123", "operador  admin operador", null);

        assertEquals("ADMIN OPERADOR", creado.getRoles());
        assertEquals("hash-nuevo", creado.getPasswordHash());
        assertTrue(creado.isActive());
        assertTrue(creado.debeCambiarClave());
        assertNull(creado.getEmail());
        verify(usuarios).save(creado);
        verify(auditorias).registrar("admin", Auditorias.USUARIO_CREADO, "ana", "roles: ADMIN OPERADOR");
        verifyNoInteractions(invitacion);
    }

    @Test
    void crearConCorreoLoGuardaSinEspaciosYEnviaLaInvitacion() {
        when(encoder.encode("clave-larga-123")).thenReturn("hash-nuevo");
        when(usuarios.findByEmailIgnoreCase("ana@ejemplo.com")).thenReturn(Optional.empty());

        Usuario creado = gestion.crear("admin", "ana", "clave-larga-123", "OPERADOR", "  ana@ejemplo.com ");

        assertEquals("ana@ejemplo.com", creado.getEmail());
        verify(invitacion).enviar("ana", "ana@ejemplo.com");
    }

    @Test
    void crearConUnCorreoDeOtraCuentaDaConflicto() {
        when(usuarios.findByEmailIgnoreCase("luis@ejemplo.com"))
                .thenReturn(Optional.of(usuario("luis", "OPERADOR", true)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> gestion.crear("admin", "ana", "clave-larga-123", "OPERADOR", "luis@ejemplo.com"));

        assertEquals(409, estado(error));
        verify(usuarios, never()).save(any());
        verifyNoInteractions(invitacion);
    }

    // --- cambiarEmail ---

    @Test
    void cambiarElCorreoDeUnUsuarioInexistenteDa404() {
        when(usuarios.findById("fantasma")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> gestion.cambiarEmail("admin", "fantasma", "x@ejemplo.com"));

        assertEquals(404, estado(error));
    }

    @Test
    void definirUnCorreoNuevoLoGuardaYAudita() {
        Usuario ana = usuario("ana", "OPERADOR", true);
        existe(ana);
        when(usuarios.findByEmailIgnoreCase("ana@ejemplo.com")).thenReturn(Optional.empty());

        gestion.cambiarEmail("admin", "ana", " ana@ejemplo.com ");

        assertEquals("ana@ejemplo.com", ana.getEmail());
        verify(auditorias).registrar("admin", Auditorias.EMAIL_CAMBIADO, "ana", "correo de contacto definido");
    }

    @Test
    void ponerElMismoCorreoNoCambiaNiAudita() {
        Usuario ana = usuario("ana", "OPERADOR", true);
        ana.cambiarEmail("ana@ejemplo.com");
        existe(ana);

        gestion.cambiarEmail("admin", "ana", "ana@ejemplo.com");

        verifyNoInteractions(auditorias);
    }

    @Test
    void retirarElCorreoLoDejaVacioYAudita() {
        Usuario ana = usuario("ana", "OPERADOR", true);
        ana.cambiarEmail("ana@ejemplo.com");
        existe(ana);

        gestion.cambiarEmail("admin", "ana", " ");

        assertNull(ana.getEmail());
        verify(auditorias).registrar("admin", Auditorias.EMAIL_CAMBIADO, "ana", "correo retirado");
    }

    @Test
    void usarElCorreoDeOtraCuentaDaConflicto() {
        Usuario ana = usuario("ana", "OPERADOR", true);
        existe(ana);
        when(usuarios.findByEmailIgnoreCase("luis@ejemplo.com"))
                .thenReturn(Optional.of(usuario("luis", "OPERADOR", true)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> gestion.cambiarEmail("admin", "ana", "luis@ejemplo.com"));

        assertEquals(409, estado(error));
        assertNull(ana.getEmail());
    }

    @Test
    void usarSuPropioCorreoRegistradoNoEsConflicto() {
        Usuario ana = usuario("ana", "OPERADOR", true);
        existe(ana);
        when(usuarios.findByEmailIgnoreCase("ana@ejemplo.com")).thenReturn(Optional.of(ana));

        gestion.cambiarEmail("admin", "ana", "ana@ejemplo.com");

        assertEquals("ana@ejemplo.com", ana.getEmail());
    }

    // --- cambiarRoles ---

    @Test
    void dejarLosMismosRolesNoHaceNadaAunqueCambieElOrden() {
        Usuario ana = usuario("ana", "ADMIN OPERADOR", true);
        existe(ana);

        gestion.cambiarRoles("admin", "ana", "operador admin");

        verifyNoInteractions(sesiones, auditorias);
    }

    @Test
    void cambiarRolesCierraLasSesionesYAudita() {
        Usuario ana = usuario("ana", "OPERADOR", true);
        existe(ana);
        when(sesiones.cerrarTodasDe("ana", null)).thenReturn(1);

        gestion.cambiarRoles("admin", "ana", "supervisor");

        assertEquals("SUPERVISOR", ana.getRoles());
        verify(auditorias).registrar("admin", Auditorias.ROL_CAMBIADO, "ana",
                "de [OPERADOR] a [SUPERVISOR]; sesiones cerradas: 1");
    }

    @Test
    void quitarleElRolAlUltimoAdministradorDaConflicto() {
        Usuario ana = usuario("ana", "ADMIN", true);
        existe(ana);
        when(usuarios.findAll()).thenReturn(List.of(ana, usuario("luis", "OPERADOR", true)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> gestion.cambiarRoles("admin", "ana", "OPERADOR"));

        assertEquals(409, estado(error));
        assertEquals("ADMIN", ana.getRoles());
        verifyNoInteractions(sesiones);
    }

    @Test
    void quitarleElRolAdminAAlguienEsPosibleSiQuedaOtroActivo() {
        Usuario ana = usuario("ana", "ADMIN", true);
        existe(ana);
        when(usuarios.findAll()).thenReturn(List.of(ana, usuario("root", "ADMIN", true)));

        gestion.cambiarRoles("admin", "ana", "OPERADOR");

        assertEquals("OPERADOR", ana.getRoles());
    }

    @Test
    void unOtroAdministradorDesactivadoNoCuentaComoRespaldo() {
        Usuario ana = usuario("ana", "ADMIN", true);
        existe(ana);
        when(usuarios.findAll()).thenReturn(List.of(ana, usuario("root", "ADMIN", false)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> gestion.cambiarRoles("admin", "ana", "OPERADOR"));

        assertEquals(409, estado(error));
    }

    // --- cambiarActivo ---

    @Test
    void dejarElMismoEstadoNoHaceNada() {
        Usuario ana = usuario("ana", "OPERADOR", true);
        existe(ana);

        gestion.cambiarActivo("admin", "ana", true);

        verifyNoInteractions(sesiones, auditorias);
    }

    @Test
    void desactivarCierraLasSesionesYAudita() {
        Usuario ana = usuario("ana", "OPERADOR", true);
        existe(ana);
        when(sesiones.cerrarTodasDe("ana", null)).thenReturn(2);

        gestion.cambiarActivo("admin", "ana", false);

        assertFalse(ana.isActive());
        verify(auditorias).registrar("admin", Auditorias.USUARIO_DESACTIVADO, "ana", "sesiones cerradas: 2");
    }

    @Test
    void desactivarAlUltimoAdministradorDaConflicto() {
        Usuario ana = usuario("ana", "ADMIN", true);
        existe(ana);
        when(usuarios.findAll()).thenReturn(List.of(ana));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> gestion.cambiarActivo("admin", "ana", false));

        assertEquals(409, estado(error));
        assertTrue(ana.isActive());
    }

    @Test
    void reactivarDesbloqueaLaCuentaYAudita() {
        Usuario ana = usuario("ana", "OPERADOR", false);
        ana.registrarIntentoFallido(5, Duration.ofMinutes(15), Duration.ofMinutes(30), AHORA);
        existe(ana);

        gestion.cambiarActivo("admin", "ana", true);

        assertTrue(ana.isActive());
        assertEquals(0, ana.getFailedAttempts());
        verify(auditorias).registrar("admin", Auditorias.USUARIO_ACTIVADO, "ana", null);
        verify(sesiones, never()).cerrarTodasDe(anyString(), any());
    }

    // --- desbloquear ---

    @Test
    void desbloquearUnaCuentaSinBloqueoNiIntentosNoAudita() {
        Usuario ana = usuario("ana", "OPERADOR", true);
        existe(ana);

        gestion.desbloquear("admin", "ana");

        verifyNoInteractions(auditorias);
    }

    @Test
    void desbloquearUnaCuentaConIntentosFallidosLaLimpiaYAudita() {
        Usuario ana = usuario("ana", "OPERADOR", true);
        ana.registrarIntentoFallido(5, Duration.ofMinutes(15), Duration.ofMinutes(30), AHORA);
        existe(ana);

        gestion.desbloquear("admin", "ana");

        assertEquals(0, ana.getFailedAttempts());
        verify(auditorias).registrar("admin", Auditorias.CUENTA_DESBLOQUEADA, "ana", null);
    }

    @Test
    void desbloquearUnaCuentaBloqueadaLaLibera() {
        Usuario ana = usuario("ana", "OPERADOR", true);
        ana.registrarIntentoFallido(1, Duration.ofMinutes(15), Duration.ofMinutes(30), AHORA);
        existe(ana);
        assertTrue(ana.bloqueado(AHORA));

        gestion.desbloquear("admin", "ana");

        assertFalse(ana.bloqueado(AHORA));
        verify(auditorias).registrar("admin", Auditorias.CUENTA_DESBLOQUEADA, "ana", null);
    }
}
