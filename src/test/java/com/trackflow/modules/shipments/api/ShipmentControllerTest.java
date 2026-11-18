package com.trackflow.modules.shipments.api;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.bootstrap.ValidationExceptionHandler;
import com.trackflow.modules.shipments.application.AdmitirEnvio;
import com.trackflow.modules.shipments.application.Comprobante;
import com.trackflow.modules.shipments.application.ConsultarComprobante;
import com.trackflow.modules.shipments.application.ConsultarFichaDeEnvio;
import com.trackflow.modules.shipments.application.EnvioSolicitado;
import com.trackflow.modules.shipments.domain.DocumentoInvalidoException;
import com.trackflow.modules.shipments.domain.NumeroDeSeguimientoInvalidoException;
import com.trackflow.modules.shipments.domain.Party;
import com.trackflow.modules.shipments.domain.ShipmentNotFoundException;
import com.trackflow.modules.shipments.domain.TipoDocumento;
import com.trackflow.shared.geografia.Ciudad;
import com.trackflow.shared.geografia.CiudadDesconocidaException;
import com.trackflow.shared.geografia.CiudadSinCoberturaException;
import com.trackflow.shared.geografia.SinRutaEntreCiudadesException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HU-01 por HTTP: admisión del envío, validación de campos y traducción de errores del dominio. */
class ShipmentControllerTest {

    private static final Ciudad MEDELLIN = new Ciudad(1L, "MEDELLÍN", "ANTIOQUIA");
    private static final Ciudad BOGOTA = new Ciudad(10L, "BOGOTÁ D.C.", "BOGOTÁ");
    private static final Instant REGISTRADO = Instant.parse("2026-10-15T18:00:00Z");

    private static final String SOLICITUD = """
            {
              "sender": { "fullName": "Ana Torres", "documentType": "CC", "documentNumber": "1000000001",
                          "phone": "3000000001", "address": "Calle 1 # 1-1", "cityId": 1 },
              "recipient": { "fullName": "Luis Gómez", "documentType": "CC", "documentNumber": "2000000002",
                             "phone": "3000000002", "address": "Carrera 2 # 2-2", "cityId": 10 },
              "description": "Documentos"
            }
            """;

    private final AdmitirEnvio admitir = mock(AdmitirEnvio.class);
    private final ConsultarComprobante consultar = mock(ConsultarComprobante.class);
    private final ConsultarFichaDeEnvio ficha = mock(ConsultarFichaDeEnvio.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ShipmentController(admitir, consultar, ficha))
            .setControllerAdvice(new ValidationExceptionHandler(), new ShipmentsExceptionHandler())
            .build();

    private ResultActions enviar(String cuerpo) throws Exception {
        return mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static Comprobante comprobante() {
        return new Comprobante("TF0123456789AB", REGISTRADO, "Ana Torres",
                "MEDELLÍN - ANTIOQUIA", "Luis Gómez", "BOGOTÁ D.C. - BOGOTÁ");
    }

    @Test
    void admiteElEnvioYRespondeAceptadoConElNumeroDeSeguimiento() throws Exception {
        EnvioSolicitado solicitud = new EnvioSolicitado("evt-1", "TF0123456789AB",
                new Party("Ana Torres", TipoDocumento.CC, "1000000001", "3000000001", "Calle 1 # 1-1", 1L),
                new Party("Luis Gómez", TipoDocumento.CC, "2000000002", "3000000002", "Carrera 2 # 2-2", 10L),
                MEDELLIN, BOGOTA, "Documentos", REGISTRADO);
        when(admitir.ejecutar(any())).thenReturn(solicitud);

        enviar(SOLICITUD)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.trackingNumber").value("TF0123456789AB"))
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.recipient").value("Luis Gómez"))
                .andExpect(jsonPath("$.destinationCity").value("BOGOTÁ D.C. - BOGOTÁ"))
                .andExpect(jsonPath("$.requestedAt").value("2026-10-15T18:00:00Z"))
                .andExpect(jsonPath("$.processingStatus").value("QUEUED"))
                .andExpect(jsonPath("$.receipt.trackingNumber").value("TF0123456789AB"))
                .andExpect(jsonPath("$.receipt.registeredAt").value("2026-10-15T18:00:00Z"))
                .andExpect(jsonPath("$.receipt.senderName").value("Ana Torres"))
                .andExpect(jsonPath("$.receipt.originCity").value("MEDELLÍN - ANTIOQUIA"))
                .andExpect(jsonPath("$.receipt.recipientName").value("Luis Gómez"))
                .andExpect(jsonPath("$.receipt.destinationCity").value("BOGOTÁ D.C. - BOGOTÁ"));

        ArgumentCaptor<AdmitirEnvio.Command> comando = ArgumentCaptor.forClass(AdmitirEnvio.Command.class);
        verify(admitir).ejecutar(comando.capture());
        assertEquals("Ana Torres", comando.getValue().remitente().nombreCompleto());
        assertEquals(TipoDocumento.CC, comando.getValue().remitente().tipoDocumento());
        assertEquals("1000000001", comando.getValue().remitente().numeroDocumento());
        assertEquals("3000000001", comando.getValue().remitente().telefono());
        assertEquals("Calle 1 # 1-1", comando.getValue().remitente().direccion());
        assertEquals(1L, comando.getValue().remitente().ciudadId());
        assertEquals(10L, comando.getValue().destinatario().ciudadId());
        assertEquals("Documentos", comando.getValue().descripcion());
    }

    @Test
    void elComprobanteSeReimprimeConLosSeisCamposYSinDatosPrivados() throws Exception {
        when(consultar.porNumero("TF0123456789AB")).thenReturn(comprobante());

        mvc.perform(get("/api/shipments/TF0123456789AB/receipt"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.trackingNumber").value("TF0123456789AB"))
                .andExpect(jsonPath("$.registeredAt").value("2026-10-15T18:00:00Z"))
                .andExpect(jsonPath("$.senderName").value("Ana Torres"))
                .andExpect(jsonPath("$.originCity").value("MEDELLÍN - ANTIOQUIA"))
                .andExpect(jsonPath("$.recipientName").value("Luis Gómez"))
                .andExpect(jsonPath("$.destinationCity").value("BOGOTÁ D.C. - BOGOTÁ"))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.documentNumber").doesNotExist());
    }

    @Test
    void unComprobanteDeUnEnvioInexistenteResponde404() throws Exception {
        when(consultar.porNumero("TF0123456789AB"))
                .thenThrow(new ShipmentNotFoundException("TF0123456789AB"));

        mvc.perform(get("/api/shipments/TF0123456789AB/receipt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Envío no encontrado"));
    }

    @Test
    void unComprobanteConFormatoInvalidoResponde400() throws Exception {
        when(consultar.porNumero("no-es-guia"))
                .thenThrow(new NumeroDeSeguimientoInvalidoException("no-es-guia"));

        mvc.perform(get("/api/shipments/no-es-guia/receipt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Número de seguimiento inválido"));
    }

    @Test
    void otroArgumentoInvalidoDelRegistroNoSeConfundeConLaGuia() throws Exception {
        when(admitir.ejecutar(any())).thenThrow(new IllegalArgumentException("dato inválido"));

        enviar(SOLICITUD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"));
    }

    @Test
    void sinDatosObligatoriosRespondeQueCamposFaltan() throws Exception {
        String incompleta = """
                {
                  "sender": { "fullName": "", "documentType": "CC", "documentNumber": "1000000001",
                              "phone": "3000000001", "address": "Calle 1", "cityId": 1 },
                  "description": " "
                }
                """;

        enviar(incompleta)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Datos obligatorios incompletos"))
                .andExpect(jsonPath("$.missingFields.recipient").value("los datos del destinatario son obligatorios"))
                .andExpect(jsonPath("$.missingFields.description").value("la descripción del paquete es obligatoria"))
                .andExpect(jsonPath("$.missingFields['sender.fullName']").value("el nombre completo es obligatorio"));

        verifyNoInteractions(admitir);
    }

    @Test
    void unTipoDeDocumentoFueraDelCatalogoRespondeValorNoAdmitido() throws Exception {
        enviar(SOLICITUD.replace("\"documentType\": \"CC\", \"documentNumber\": \"1000000001\"",
                "\"documentType\": \"DNI\", \"documentNumber\": \"1000000001\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Valor no admitido"))
                .andExpect(jsonPath("$.cause").value(containsString("DNI")));

        verifyNoInteractions(admitir);
    }

    @Test
    void unDocumentoInvalidoResponde400() throws Exception {
        when(admitir.ejecutar(any())).thenThrow(new DocumentoInvalidoException(TipoDocumento.CC, "12"));

        enviar(SOLICITUD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Documento de identidad inválido"))
                .andExpect(jsonPath("$.detail").value(containsString("'12'")));
    }

    @Test
    void unaCiudadDesconocidaResponde400() throws Exception {
        when(admitir.ejecutar(any())).thenThrow(new CiudadDesconocidaException(99L));

        enviar(SOLICITUD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ciudad no encontrada en el catálogo"))
                .andExpect(jsonPath("$.detail").value(containsString("La ciudad 99")));
    }

    @Test
    void unaCiudadSinCoberturaResponde422() throws Exception {
        when(admitir.ejecutar(any())).thenThrow(new CiudadSinCoberturaException(BOGOTA));

        enviar(SOLICITUD)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Ciudad sin cobertura"))
                .andExpect(jsonPath("$.detail").value(containsString("BOGOTÁ D.C. - BOGOTÁ")));
    }

    @Test
    void ciudadesSinRutaEntreEllasResponden422() throws Exception {
        when(admitir.ejecutar(any())).thenThrow(new SinRutaEntreCiudadesException(MEDELLIN, BOGOTA));

        enviar(SOLICITUD)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Sin ruta entre las ciudades"))
                .andExpect(jsonPath("$.detail").value(
                        "La red no tiene una ruta de MEDELLÍN - ANTIOQUIA a BOGOTÁ D.C. - BOGOTÁ, así que el envío no podría salir del origen"));
    }
}
