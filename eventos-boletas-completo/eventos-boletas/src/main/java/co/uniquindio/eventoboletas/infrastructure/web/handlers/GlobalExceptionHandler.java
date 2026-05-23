package co.uniquindio.eventoboletas.infrastructure.web.handlers;

import co.uniquindio.eventoboletas.domain.exceptions.EntidadNoEncontradaException;
import co.uniquindio.eventoboletas.domain.exceptions.ReglaDeNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones.
 *
 * RNF-01: mensajes de error claros junto al campo que falla.
 * Patrón: centraliza el manejo de errores (Single Responsibility).
 *
 * Respuestas:
 * - 400 Bad Request → validación Bean Validation (campo por campo)
 * - 404 Not Found   → EntidadNoEncontradaException
 * - 422 Unprocessable Entity → ReglaDeNegocioException (reglas de negocio)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** RNF-01: errores de validación por campo (Bean Validation) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> erroresCampos = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            erroresCampos.put(campo, error.getDefaultMessage());
        });

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 400);
        body.put("error", "Datos inválidos");
        body.put("campos", erroresCampos);
        return ResponseEntity.badRequest().body(body);
    }

    /** RN-01, RN-02, RN-03, RN-05, RN-06, RN-07: reglas de negocio violadas */
    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleReglaNegocio(ReglaDeNegocioException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 422);
        body.put("error", "Regla de negocio");
        body.put("mensaje", ex.getMessage());
        return ResponseEntity.unprocessableEntity().body(body);
    }

    /** Entidad no encontrada */
    @ExceptionHandler(EntidadNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrado(EntidadNoEncontradaException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 404);
        body.put("error", "Entidad no encontrada");
        body.put("mensaje", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /** Fallback */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 500);
        body.put("error", "Error interno del servidor");
        body.put("mensaje", ex.getMessage());
        return ResponseEntity.internalServerError().body(body);
    }
}
