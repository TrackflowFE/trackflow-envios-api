package com.trackflow.modules.pickups.api;

import com.trackflow.modules.pickups.domain.DiaNoOperativoException;
import com.trackflow.modules.pickups.domain.EnvioVinculadoFueraDeCiudadException;
import com.trackflow.modules.pickups.domain.EnvioVinculadoNoEncontradoException;
import com.trackflow.modules.pickups.domain.FechaDeRecogidaEnElPasadoException;
import com.trackflow.modules.pickups.domain.FechaFueraDelHorizonteException;
import com.trackflow.modules.pickups.domain.FranjaDesconocidaException;
import com.trackflow.modules.pickups.domain.OperadorDesconocidoException;
import com.trackflow.modules.pickups.domain.RecogidaNoEncontradaException;
import com.trackflow.modules.pickups.domain.SinAnticipacionSuficienteException;
import com.trackflow.modules.pickups.domain.TransicionDeRecogidaInvalidaException;
import com.trackflow.shared.geografia.CiudadDesconocidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones del dominio de recogidas a respuestas HTTP.
 *
 * Vive en api/ para que el dominio no tenga que conocer el protocolo. El motivo va
 * en el detalle, porque quien pide la recogida necesita saber qué corregir: una
 * fecha que ya pasó y una franja que no existe son problemas distintos.
 */
@RestControllerAdvice(assignableTypes = PickupController.class)
public class PickupsExceptionHandler {

    @ExceptionHandler(RecogidaNoEncontradaException.class)
    ProblemDetail recogidaNoEncontrada(RecogidaNoEncontradaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problema.setTitle("Recogida no encontrada");
        return problema;
    }

    @ExceptionHandler(TransicionDeRecogidaInvalidaException.class)
    ProblemDetail transicionInvalida(TransicionDeRecogidaInvalidaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problema.setTitle("La recogida no está en un estado que admita ese cambio");
        return problema;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflicto(IllegalStateException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problema.setTitle("Conflicto con la recogida");
        return problema;
    }

    @ExceptionHandler(FechaDeRecogidaEnElPasadoException.class)
    ProblemDetail fechaEnElPasado(FechaDeRecogidaEnElPasadoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setTitle("La fecha de la recogida ya pasó");
        return problema;
    }

    @ExceptionHandler(DiaNoOperativoException.class)
    ProblemDetail diaNoOperativo(DiaNoOperativoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("No se recoge ese día");
        return problema;
    }

    @ExceptionHandler(FechaFueraDelHorizonteException.class)
    ProblemDetail fueraDelHorizonte(FechaFueraDelHorizonteException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("La fecha está fuera del horizonte de programación");
        return problema;
    }

    @ExceptionHandler(FranjaDesconocidaException.class)
    ProblemDetail franjaDesconocida(FranjaDesconocidaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("La franja no existe en el horario de atención");
        return problema;
    }

    @ExceptionHandler(SinAnticipacionSuficienteException.class)
    ProblemDetail sinAnticipacion(SinAnticipacionSuficienteException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("No hay tiempo para atender esa franja");
        return problema;
    }

    @ExceptionHandler(EnvioVinculadoNoEncontradoException.class)
    ProblemDetail envioNoEncontrado(EnvioVinculadoNoEncontradoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problema.setTitle("El envío no existe");
        return problema;
    }

    @ExceptionHandler(EnvioVinculadoFueraDeCiudadException.class)
    ProblemDetail envioFueraDeCiudad(EnvioVinculadoFueraDeCiudadException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("El envío no sale de la ciudad de la recogida");
        return problema;
    }

    @ExceptionHandler(OperadorDesconocidoException.class)
    ProblemDetail operadorDesconocido(OperadorDesconocidoException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        problema.setTitle("No se puede asignar la recogida a ese usuario");
        return problema;
    }

    @ExceptionHandler(CiudadDesconocidaException.class)
    ProblemDetail ciudadDesconocida(CiudadDesconocidaException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setTitle("Ciudad no encontrada en el catálogo");
        return problema;
    }

    /** Un número de recogida con otro formato no es una recogida: es un dato inválido. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail numeroInvalido(IllegalArgumentException e) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problema.setTitle("Número de recogida inválido");
        return problema;
    }
}
