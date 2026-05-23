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

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<Map<String, String>> handleReglaDeNegocio(ReglaDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(EntidadNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleNoEncontrada(EntidadNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", ex.getMessage()));
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
