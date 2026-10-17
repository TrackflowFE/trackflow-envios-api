package com.trackflow.modules.shipments.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.trackflow.modules.shipments.application.EnvioSolicitado;
import com.trackflow.modules.shipments.domain.DocumentoInvalidoException;
import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.shared.geografia.Ciudad;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** El contrato del mensaje de solicitud: conversión desde y hacia el dominio y lectura tolerante del JSON. */
class EnvioSolicitadoMensajeTest {

    private static final Instant SOLICITADO = Instant.parse("2026-10-15T18:00:00Z");

    private final JsonMapper json = JsonMapper.builder().build();

    private final EnvioSolicitado solicitud = new EnvioSolicitado("evt-1", "TF0123456789AB",
            new Party("Ana Torres", TipoDocumento.CC, "1000000001", "3000000001", "Calle 1 # 1-1", 1L),
            new Party("Luis Gómez", TipoDocumento.NIT, "900123456", "3000000002", "Carrera 2 # 2-2", 10L),
            new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA"),
            new Ciudad(10L, "BOGOTÁ D.C.", "BOGOTÁ"),
            "Documentos",
            SOLICITADO);

    @Test
    void seConstruyeDesdeLaSolicitudConTodosSusDatos() {
        EnvioSolicitadoMensaje mensaje = EnvioSolicitadoMensaje.from(solicitud);

        assertEquals("evt-1", mensaje.eventId());
        assertEquals("TF0123456789AB", mensaje.trackingNumber());
        assertEquals(new EnvioSolicitadoMensaje.PersonaMensaje("Ana Torres", "CC", "1000000001", "3000000001",
                "Calle 1 # 1-1", 1L), mensaje.sender());
        assertEquals("NIT", mensaje.recipient().documentType());
        assertEquals(new EnvioSolicitadoMensaje.CiudadMensaje(1L, "MEDELLÍN", "ANTIOQUIA"), mensaje.originCity());
        assertEquals(new EnvioSolicitadoMensaje.CiudadMensaje(10L, "BOGOTÁ D.C.", "BOGOTÁ"),
                mensaje.destinationCity());
        assertEquals("Documentos", mensaje.description());
        assertEquals(SOLICITADO, mensaje.requestedAt());
        assertEquals(EnvioSolicitadoMensaje.VERSION, mensaje.schemaVersion());
    }

    @Test
    void vuelveALaSolicitudSinPerderDatos() {
        EnvioSolicitado reconstruida = EnvioSolicitadoMensaje.from(solicitud).toSolicitud();

        assertEquals(solicitud.eventId(), reconstruida.eventId());
        assertEquals(solicitud.trackingNumber(), reconstruida.trackingNumber());
        assertEquals(solicitud.ciudadOrigen(), reconstruida.ciudadOrigen());
        assertEquals(solicitud.ciudadDestino(), reconstruida.ciudadDestino());
        assertEquals(solicitud.descripcion(), reconstruida.descripcion());
        assertEquals(solicitud.solicitadoEn(), reconstruida.solicitadoEn());
        assertEquals("Ana Torres", reconstruida.remitente().getFullName());
        assertEquals(TipoDocumento.CC, reconstruida.remitente().getDocumentType());
        assertEquals("1000000001", reconstruida.remitente().getDocumentNumber());
        assertEquals("3000000001", reconstruida.remitente().getPhone());
        assertEquals("Calle 1 # 1-1", reconstruida.remitente().getAddress());
        assertEquals(1L, reconstruida.remitente().getCityId());
        assertEquals(TipoDocumento.NIT, reconstruida.destinatario().getDocumentType());
    }

    @Test
    void alSerializarIncluyeLaVersionDelFormato() {
        String texto = json.writeValueAsString(EnvioSolicitadoMensaje.from(solicitud));

        assertTrue(texto.contains("\"version\":1"), texto);
        assertTrue(texto.contains("\"trackingNumber\":\"TF0123456789AB\""), texto);
        assertTrue(texto.contains("\"sender\""), texto);
    }

    @Test
    void entiendeLosNombresEnEspanolEIgnoraCamposDesconocidos() {
        String viejo = """
                {"eventId":"evt-9","trackingNumber":"TF0123456789AB","version":1,"campoNuevo":"x",
                 "remitente":{"nombreCompleto":"Ana","tipoDocumento":"CC","numeroDocumento":"1000000001",
                              "telefono":"300","direccion":"Calle 1","ciudadId":1,"otro":true},
                 "destinatario":{"nombreCompleto":"Luis","tipoDocumento":"CC","numeroDocumento":"2000000002",
                                 "telefono":"301","direccion":"Carrera 2","ciudadId":10},
                 "ciudadOrigen":{"id":1,"nombre":"MEDELLÍN","departamento":"ANTIOQUIA"},
                 "ciudadDestino":{"id":10,"nombre":"BOGOTÁ D.C.","departamento":"BOGOTÁ"},
                 "descripcion":"Documentos","solicitadoEn":"2026-10-15T18:00:00Z"}
                """;

        EnvioSolicitado leida = json.readValue(viejo, EnvioSolicitadoMensaje.class).toSolicitud();

        assertEquals("evt-9", leida.eventId());
        assertEquals("Ana", leida.remitente().getFullName());
        assertEquals("Carrera 2", leida.destinatario().getAddress());
        assertEquals(new Ciudad(10L, "BOGOTÁ D.C.", "BOGOTÁ"), leida.ciudadDestino());
        assertEquals("Documentos", leida.descripcion());
        assertEquals(SOLICITADO, leida.solicitadoEn());
    }

    @Test
    void unMensajeConDocumentoInvalidoNoSeConvierteEnSolicitud() {
        EnvioSolicitadoMensaje.PersonaMensaje mala = new EnvioSolicitadoMensaje.PersonaMensaje("Ana", "CC", "12",
                "300", "Calle", 1L);
        EnvioSolicitadoMensaje mensaje = new EnvioSolicitadoMensaje("evt", "TF0123456789AB", mala, mala,
                new EnvioSolicitadoMensaje.CiudadMensaje(1L, "A", "B"),
                new EnvioSolicitadoMensaje.CiudadMensaje(1L, "A", "B"), "x", SOLICITADO);

        assertThrows(DocumentoInvalidoException.class, mensaje::toSolicitud);
    }

    @Test
    void unTipoDeDocumentoFueraDelCatalogoNoSeConvierte() {
        EnvioSolicitadoMensaje.PersonaMensaje mala = new EnvioSolicitadoMensaje.PersonaMensaje("Ana", "DNI",
                "1000000001", "300", "Calle", 1L);
        EnvioSolicitadoMensaje mensaje = new EnvioSolicitadoMensaje("evt", "TF0123456789AB", mala, mala,
                new EnvioSolicitadoMensaje.CiudadMensaje(1L, "A", "B"),
                new EnvioSolicitadoMensaje.CiudadMensaje(1L, "A", "B"), "x", SOLICITADO);

        assertThrows(IllegalArgumentException.class, mensaje::toSolicitud);
    }
}
