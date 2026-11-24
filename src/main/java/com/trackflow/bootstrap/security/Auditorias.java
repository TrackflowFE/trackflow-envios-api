package com.trackflow.bootstrap.security;

import com.trackflow.shared.seguridad.IpDelCliente;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deja constancia de los cambios sobre usuarios, roles y permisos (HU-11).
 *
 * Nunca recibe contraseñas ni hashes: el detalle describe qué cambió, no con qué
 * valor, para que el propio registro no se convierta en una filtración.
 */
@Service
public class Auditorias {

    /** Acciones registradas, para que el detalle no sea texto libre sin control. */
    public static final String USUARIO_CREADO = "USUARIO_CREADO";
    public static final String ROL_CAMBIADO = "ROL_CAMBIADO";
    public static final String USUARIO_ACTIVADO = "USUARIO_ACTIVADO";
    public static final String USUARIO_DESACTIVADO = "USUARIO_DESACTIVADO";
    public static final String CUENTA_DESBLOQUEADA = "CUENTA_DESBLOQUEADA";
    public static final String CUENTA_BLOQUEADA = "CUENTA_BLOQUEADA";
    public static final String CLAVE_CAMBIADA = "CLAVE_CAMBIADA";
    /** Cambió el correo de contacto, que es por donde se recupera la clave (HU-10). */
    public static final String EMAIL_CAMBIADO = "EMAIL_CAMBIADO";
    /** Alguien vio los datos personales completos de un envío (HU-13). */
    public static final String DATOS_COMPLETOS_CONSULTADOS = "DATOS_COMPLETOS_CONSULTADOS";

    /** Cuando el cambio no lo hace una persona, sino el arranque de la aplicación. */
    public static final String SISTEMA = "sistema";

    private final AuditoriaRepository repositorio;
    private final Clock clock;
    private final IpDelCliente ipDelCliente;

    public Auditorias(AuditoriaRepository repositorio, Clock clock, IpDelCliente ipDelCliente) {
        this.repositorio = repositorio;
        this.clock = clock;
        this.ipDelCliente = ipDelCliente;
    }

    @Transactional
    public void registrar(String actor, String accion, String objetivo, String detalle) {
        repositorio.save(new Auditoria(actor, accion, objetivo, detalle, clock.instant(), ipDeLaPeticion()));
    }

    /**
     * La IP se toma del hilo de la petición en curso y no como parámetro, para que las
     * llamadas que ya existen no tengan que arrastrarla por media aplicación.
     *
     * Devuelve null cuando no hay petición: el arranque que crea los usuarios iniciales,
     * o un envío de correo asíncrono. Un registro sin IP es correcto; uno con la IP
     * equivocada no lo sería.
     */
    private String ipDeLaPeticion() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return ipDelCliente.obtener(atributos.getRequest());
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<Auditoria> ultimos(int cuantos) {
        return repositorio.findAllByOrderByOcurridoEnDesc(Limit.of(cuantos));
    }
}
