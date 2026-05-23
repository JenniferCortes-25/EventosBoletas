package co.uniquindio.eventoboletas.infrastructure.web.handlers.chain;

import org.springframework.http.ResponseEntity;
import java.util.Map;

/**
 * Patrón Chain of Responsibility — eslabón base.
 * Cada handler concreto decide si atiende la excepción o la pasa al siguiente.
 */
public abstract class ExceptionHandlerLink {

    private ExceptionHandlerLink siguiente;

    public ExceptionHandlerLink setSiguiente(ExceptionHandlerLink siguiente) {
        this.siguiente = siguiente;
        return siguiente;          // permite encadenar fluido: a.setSiguiente(b).setSiguiente(c)
    }

    /**
     * Intenta manejar la excepción. Si no puede, delega al siguiente eslabón.
     */
    public final ResponseEntity<Map<String, Object>> manejar(Exception ex) {
        if (puedeAtender(ex)) {
            return atender(ex);
        }
        if (siguiente != null) {
            return siguiente.manejar(ex);
        }
        // Fallback si la cadena se agota sin atender
        return ResponseEntity.internalServerError()
            .body(Map.of("error", "Error inesperado", "mensaje", ex.getMessage()));
    }

    protected abstract boolean puedeAtender(Exception ex);
    protected abstract ResponseEntity<Map<String, Object>> atender(Exception ex);
}