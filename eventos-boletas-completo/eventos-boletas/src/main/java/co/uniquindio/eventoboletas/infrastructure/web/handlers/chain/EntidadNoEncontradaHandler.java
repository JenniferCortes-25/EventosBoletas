package co.uniquindio.eventoboletas.infrastructure.web.handlers.chain;

import co.uniquindio.eventoboletas.domain.exceptions.EntidadNoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/** Eslabón 3: maneja entidades no encontradas (404). */
public class EntidadNoEncontradaHandler extends ExceptionHandlerLink {

    @Override
    protected boolean puedeAtender(Exception ex) {
        return ex instanceof EntidadNoEncontradaException;
    }

    @Override
    protected ResponseEntity<Map<String, Object>> atender(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", ex.getMessage()));
    }
}