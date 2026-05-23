package co.uniquindio.eventoboletas.infrastructure.web.handlers.chain;

import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/** Eslabón 2: maneja violaciones de reglas de negocio (422). */
public class ReglaDeNegocioHandler extends ExceptionHandlerLink {

    @Override
    protected boolean puedeAtender(Exception ex) {
        return ex instanceof ReglaDeNegocioException;
    }

    @Override
    protected ResponseEntity<Map<String, Object>> atender(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(Map.of("message", ex.getMessage()));
    }
}