package co.uniquindio.eventoboletas.infrastructure.web.handlers.chain;

import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Eslabón final: captura cualquier excepción no atendida (500). */
public class FallbackHandler extends ExceptionHandlerLink {

    @Override
    protected boolean puedeAtender(Exception ex) {
        return true; // siempre atiende — es el último eslabón
    }

    @Override
    protected ResponseEntity<Map<String, Object>> atender(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 500);
        body.put("error", "Error interno del servidor");
        body.put("mensaje", ex.getMessage());
        return ResponseEntity.internalServerError().body(body);
    }
}