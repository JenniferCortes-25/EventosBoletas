package co.uniquindio.eventoboletas.infrastructure.web.handlers;

import co.uniquindio.eventoboletas.infrastructure.web.handlers.chain.*;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Patrón Chain of Responsibility — ensamblador de la cadena.
 *
 * La cadena es: ValidacionHandler → ReglaDeNegocioHandler
 *               → EntidadNoEncontradaHandler → FallbackHandler
 *
 * Cada eslabón revisa si puede atender la excepción (puedeAtender()).
 * Si no puede, la pasa al siguiente. El FallbackHandler siempre atiende.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ExceptionHandlerLink cadena;

    @PostConstruct
    public void construirCadena() {
        ValidacionHandler validacion       = new ValidacionHandler();
        ReglaDeNegocioHandler regla        = new ReglaDeNegocioHandler();
        EntidadNoEncontradaHandler noEnc   = new EntidadNoEncontradaHandler();
        FallbackHandler fallback           = new FallbackHandler();

        // Encadenamiento: validacion → regla → noEncontrada → fallback
        validacion.setSiguiente(regla)
                  .setSiguiente(noEnc)
                  .setSiguiente(fallback);

        cadena = validacion;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejar(Exception ex) {
        return cadena.manejar(ex);
    }
}
